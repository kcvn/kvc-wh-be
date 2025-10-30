package com.kcvn.spm.app.transaction.receiving.service

import com.kcvn.spm.app.backlogwh.service.BacklogWhService
import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransRequest
import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransRequestWithSeq
import com.kcvn.spm.app.transaction.receiving.payload.request.RecTransSearchRequest
import com.kcvn.spm.app.transaction.receiving.payload.response.RecTransResponse
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.ReceivingTransactions
import com.kcvn.spm.repository.ReceivingTransactionsRepository
import com.kcvn.spm.repository.SplittingRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
@Transactional
class ReceivingTransactionsService(
    private val receivingRepo: ReceivingTransactionsRepository,
    private val backlogWhService: BacklogWhService,
    private val splittingRepo: SplittingRepository
) {
    fun getList(request: RecTransSearchRequest, pageable: Pageable): BasePagingResponse<RecTransResponse> {
        val recTrans = receivingRepo.getList(request, pageable)
        val data = recTrans.first.map {
            RecTransResponse(
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
            recTrans.second
        )
    }

    fun saveRecTrans(request: List<RecTransRequest>, receivingDate: LocalDate?) {
        val list = createRecTransRequestWithSeq(request)
        list.forEach {
            val recTransaction = ReceivingTransactions(
                null,
                "KVC",
                it.locationCode,
                it.packageCode,
                it.packageCode,
                it.poNumber,
                it.qty,
                it.seqNo,
                "IN_ONLY"
            )
            /*val splitting = splittingRepo.findByLocationAndPackage(it.locationCode!!, it.packageCode!!)
                ?: throw BusinessExceptionDetail(CommonUtils.getMessage("data.not.found.in.splitting"), "locationCode = ${it.locationCode}, packageCode = ${it.packageCode}")*/
            // save receiving transactions
            receivingRepo.save(recTransaction)
            // save backlog and backlog history
            val backlogData = BacklogWh(
                null, it.locationCode, it.poNumber, it.packageCode, it.qty, 1, receivingDate, isEntried = false
            )
            backlogWhService.plusBacklog(backlogData, "IN_ONLY")
        }
    }

    fun createRecTransRequestWithSeq(requests: List<RecTransRequest>): List<RecTransRequestWithSeq> {
        val todayUtc = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate()
        return requests
            .groupBy { it.locationCode to it.poNumber }
            .flatMap { (key, group) ->
                val (locationCode, poNumber) = key
                val latestSeqNo = receivingRepo.findLatestByLocationCodeAndPO(locationCode!!, poNumber!!, todayUtc)?.seqNo ?: 0

                group.mapIndexed { index, recTransRequest ->
                    RecTransRequestWithSeq(
                        locationCode = recTransRequest.locationCode,
                        packageCode = recTransRequest.packageCode,
                        poNumber = recTransRequest.poNumber,
                        qty = recTransRequest.qty,
                        seqNo = latestSeqNo + index + 1 // Bắt đầu từ latestSeqNo + 1, tăng dần
                    )
                }
            }
    }
}