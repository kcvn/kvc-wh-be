package com.kcvn.spm.app.transaction.sending.service

import com.kcvn.spm.app.backlogwh.service.BacklogWhService
import com.kcvn.spm.app.transaction.receiving.service.ReceivingTransactionsService
import com.kcvn.spm.app.transaction.sending.payload.request.SendTransRequestWithSeq
import com.kcvn.spm.app.transaction.sending.payload.request.SendingRequest
import com.kcvn.spm.app.transaction.sending.payload.request.SendingSearchRequest
import com.kcvn.spm.app.transaction.sending.payload.request.ValidateSendTransRequest
import com.kcvn.spm.app.transaction.sending.payload.response.SendingResponse
import com.kcvn.spm.app.transaction.sending.payload.response.ValidateSendTransResponse
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.SendingTransactions
import com.kcvn.spm.repository.SendingTransactionsRepository
import com.kcvn.spm.repository.SplittingRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.math.BigDecimal
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
@Transactional
class SendingTransactionsService(
    private val sendingRepo: SendingTransactionsRepository,
    private val backlogWhService: BacklogWhService,
    private val receivingService: ReceivingTransactionsService,
    private val splittingRepo: SplittingRepository
) {
    fun getList(request: SendingSearchRequest, pageable: Pageable): BasePagingResponse<SendingResponse> {
        val moving = sendingRepo.getList(request, pageable)
        val data = moving.first.map {
            SendingResponse(
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

    fun validateSourceBacklogFromSending(request: List<SendingRequest>): List<ValidateSendTransResponse> {
        val list = aggregateSendingRequests(request)
        val response = mutableListOf<ValidateSendTransResponse>()
        list.forEach {
            val backlog = backlogWhService.getByLocationAndPackageAndPO(it.sourceLocationCode!!, it.sourcePackageCode!!, it.poNumber!!)
            if (it.qty!! > backlog.backlogQty!!) {
                val vmr = ValidateSendTransResponse(
                    sourceLocationCode = it.sourceLocationCode,
                    sourcePackageCode = it.sourcePackageCode,
                    poNumber = it.poNumber
                )
                response.add(vmr)
            }
        }
        return response
    }

    fun aggregateSendingRequests(sendingRequests: List<SendingRequest>): List<ValidateSendTransRequest> {
        return sendingRequests
            .groupBy { Triple(it.locationCode, it.packageCode, it.poNumber) }
            .map { (key, group) ->
                ValidateSendTransRequest(
                    sourceLocationCode = key.first,
                    sourcePackageCode = key.second,
                    poNumber = key.third,
                    qty = group.sumOf { it.qty ?: BigDecimal.ZERO }
                )
            }
    }

    fun saveSendTrans(request: List<SendingRequest>) {
        val list = createSendTransRequestWithSeq(request)
        list.forEach {
            // get receivingDate
            val splittingSource = splittingRepo.findByLocationAndPackage(it.sourceLocationCode!!, it.packageCode!!)
                ?: throw BusinessExceptionDetail(
                    CommonUtils.getMessage("data.not.found.in.splitting"), "locationCode = ${it.sourceLocationCode}, packageCode = ${it.packageCode}"
                )
            val receivingDate = splittingSource.receivingDate
            val sendTran = SendingTransactions(
                null,
                it.sourceLocationCode,
                "KVC",
                it.packageCode,
                it.packageCode,
                it.poNumber,
                it.qty,
                it.seqNo,
                "OUT_ONLY",
                receivingDate
            )
            sendingRepo.saveSendingTrans(sendTran)
            // save backlog and backlog history
            val backlogData = BacklogWh(
                null,
                it.sourceLocationCode,
                it.poNumber,
                it.packageCode,
                it.qty,
                1,
                receivingDate
            )
            backlogWhService.minusBacklog(backlogData, "OUT_ONLY")
        }
    }

    fun createSendTransRequestWithSeq(requests: List<SendingRequest>): List<SendTransRequestWithSeq> {
        val todayUtc = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate()
        return requests
            .groupBy { Triple(it.locationCode, it.packageCode, it.poNumber) }
            .flatMap { (key, group) ->
                val (sourceLocationCode, sourcePackageCode, poNumber) = key
                val latestSeqNo = sendingRepo.findLatestMoving(sourceLocationCode!!, sourcePackageCode!!, poNumber!!, todayUtc)?.seqNo ?: 0

                group.mapIndexed { index, sendTransRequest ->
                    SendTransRequestWithSeq(
                        inspectionDate = sendTransRequest.inspectionDate,
                        sourceLocationCode = sendTransRequest.locationCode,
                        packageCode = sendTransRequest.packageCode,
                        poNumber = sendTransRequest.poNumber,
                        qty = sendTransRequest.qty,
                        seqNo = latestSeqNo + index + 1 // Bắt đầu từ latestSeqNo + 1, tăng dần
                    )
                }
            }
    }
}