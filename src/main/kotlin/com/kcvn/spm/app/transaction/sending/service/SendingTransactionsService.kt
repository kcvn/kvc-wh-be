package com.kcvn.spm.app.transaction.sending.service

import com.kcvn.spm.app.backlog.service.BacklogService
import com.kcvn.spm.app.transaction.receiving.service.ReceivingTransactionsService
import com.kcvn.spm.app.transaction.sending.payload.request.MovingRequest
import com.kcvn.spm.app.transaction.sending.payload.request.MovingRequestWithSeq
import com.kcvn.spm.app.transaction.sending.payload.request.MovingSearchRequest
import com.kcvn.spm.app.transaction.sending.payload.request.ValidateMovingRequest
import com.kcvn.spm.app.transaction.sending.payload.response.MovingResponse
import com.kcvn.spm.app.transaction.sending.payload.response.ValidateMovingResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.model.tables.pojos.Backlog
import com.kcvn.spm.model.tables.pojos.SendingTransactions
import com.kcvn.spm.repository.SendingTransactionsRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
@Transactional
class SendingTransactionsService(
    private val sendingRepo: SendingTransactionsRepository,
    private val backlogService: BacklogService,
    private val receivingService: ReceivingTransactionsService
) {
    fun getList(request: MovingSearchRequest, pageable: Pageable): BasePagingResponse<MovingResponse> {
        val moving = sendingRepo.getList(request, pageable)
        val data = moving.first.map {
            MovingResponse(
                sourceLocationCode = it.sourceLocationCode,
                destLocationCode = it.destLocationCode,
                poNumber = it.poNumber,
                qty = it.qty,
                seq = it.seqNo,
                createdDate = it.createdDate
            )
        }
        return BasePagingResponse(
            data,
            moving.second
        )
    }

    fun validateSourceBacklog(request: List<MovingRequest>): List<ValidateMovingResponse> {
        val list = aggregateMovingRequests(request)
        val response = mutableListOf<ValidateMovingResponse>()
        list.forEach {
            val backlog = backlogService.getByLocationCodeAndPO(it.sourceLocationCode!!, it.poNumber!!)
            if (it.qty!! > backlog.backlogQty!!) {
                val vmr = ValidateMovingResponse(
                    sourceLocationCode = it.sourceLocationCode,
                    poNumber = it.poNumber
                )
                response.add(vmr)
            }
        }
        return response
    }

    fun aggregateMovingRequests(movingRequests: List<MovingRequest>): List<ValidateMovingRequest> {
        return movingRequests
            .groupBy { it.sourceLocationCode to it.poNumber }
            .map { (key, group) ->
                ValidateMovingRequest(
                    sourceLocationCode = key.first,
                    poNumber = key.second,
                    qty = group.sumOf { it.qty ?: 0 }
                )
            }
    }

    fun saveMoving(request: List<MovingRequest>) {
        val todayUtc = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate()
        val list = createMovingRequestWithSeq(request, todayUtc)
        list.forEach {
            // save receiving transaction
            val seqReceiving = receivingService.saveRecTransFromMoving(it, todayUtc)
            // save moving
            val moving = SendingTransactions(
                null,
                it.sourceLocationCode,
                it.destLocationCode,
                it.poNumber,
                it.qty,
                it.seqNo,
                seqReceiving
            )
            sendingRepo.saveMoving(moving)
            // plus backlog destLocation
            val backlogDestData = Backlog(
                null,
                it.destLocationCode,
                it.poNumber,
                it.qty,
                null
            )
            backlogService.plusBacklog(backlogDestData, "IN")
            // minus backlog sourceLocation
            val backlogSourceData = Backlog(
                null,
                it.sourceLocationCode,
                it.poNumber,
                it.qty,
                null
            )
            backlogService.minusBacklog(backlogSourceData, "OUT")
        }
    }

    fun createMovingRequestWithSeq(requests: List<MovingRequest>, todayUtc: LocalDate): List<MovingRequestWithSeq> {
        return requests
            .groupBy { it.sourceLocationCode to it.poNumber }
            .flatMap { (key, group) ->
                val (sourceLocationCode, poNumber) = key
                val latestSeqNo = sendingRepo.findLatestMoving(sourceLocationCode!!, poNumber!!, todayUtc)?.seqNo ?: 0

                group.mapIndexed { index, movingRequest ->
                    MovingRequestWithSeq(
                        sourceLocationCode = movingRequest.sourceLocationCode,
                        destLocationCode = movingRequest.destLocationCode,
                        poNumber = movingRequest.poNumber,
                        qty = movingRequest.qty,
                        seqNo = latestSeqNo + index + 1 // Bắt đầu từ latestSeqNo + 1, tăng dần
                    )
                }
            }
    }
}