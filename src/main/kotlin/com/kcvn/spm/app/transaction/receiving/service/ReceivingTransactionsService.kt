package com.kcvn.spm.app.transaction.receiving.service

import com.kcvn.spm.app.transaction.receiving.payload.RecTransRequest
import com.kcvn.spm.app.transaction.receiving.payload.RecTransRequestWithSeq
import com.kcvn.spm.model.tables.pojos.Backlog
import com.kcvn.spm.model.tables.pojos.BacklogHistory
import com.kcvn.spm.model.tables.pojos.ReceivingTransactions
import com.kcvn.spm.repository.BacklogHistoryRepository
import com.kcvn.spm.repository.BacklogRepository
import com.kcvn.spm.repository.ReceivingTransactionsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ReceivingTransactionsService(
    private val receivingRepo: ReceivingTransactionsRepository,
    private val backlogRepo: BacklogRepository,
    private val backlogHistoryRepo: BacklogHistoryRepository
) {
    fun saveRecTrans(request: List<RecTransRequest>) {
        val list = createRecTransRequestWithSeq(request)
        list.forEach {
            val recTransaction = ReceivingTransactions(
                null,
                it.locationCode,
                it.poNumber,
                it.qty,
                it.seqNo
            )
            // save receiving transactions
            receivingRepo.save(recTransaction)
            // save backlog and backlog history
            val backlog = backlogRepo.findByLocationCodeAndPO(it.locationCode!!, it.poNumber!!)
            if (backlog == null) {
                val entityBacklog = Backlog(null, it.locationCode, it.poNumber, it.qty, 1)
                backlogRepo.save(entityBacklog)
                val entityBacklogHistory = BacklogHistory(null, it.locationCode, it.poNumber, it.qty, 1, "IN_ONLY")
                backlogHistoryRepo.save(entityBacklogHistory)
            } else {
                val entityBacklog = Backlog(null, it.locationCode, it.poNumber, it.qty?.plus(backlog.backlogQty!!), 1.plus(backlog.boxQty!!))
                backlogRepo.update(entityBacklog)
                val entityBacklogHistory = BacklogHistory(
                    null, it.locationCode, it.poNumber, it.qty?.plus(backlog.backlogQty!!), 1.plus(backlog.boxQty!!), "IN_ONLY"
                )
                backlogHistoryRepo.save(entityBacklogHistory)
            }
        }
    }

    fun createRecTransRequestWithSeq(requests: List<RecTransRequest>): List<RecTransRequestWithSeq> {
        return requests
            .groupBy { it.locationCode to it.poNumber }
            .flatMap { (key, group) ->
                val (locationCode, poNumber) = key
                val latestSeqNo = receivingRepo.findLatestByLocationCodeAndPO(locationCode!!, poNumber!!)?.seqNo ?: 0

                group.mapIndexed { index, recTransRequest ->
                    RecTransRequestWithSeq(
                        locationCode = recTransRequest.locationCode,
                        poNumber = recTransRequest.poNumber,
                        qty = recTransRequest.qty,
                        seqNo = latestSeqNo + index + 1 // Bắt đầu từ latestSeqNo + 1, tăng dần
                    )
                }
            }
    }
}