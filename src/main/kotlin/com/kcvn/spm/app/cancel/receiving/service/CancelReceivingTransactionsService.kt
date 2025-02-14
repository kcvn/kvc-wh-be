package com.kcvn.spm.app.cancel.receiving.service

import com.kcvn.spm.app.cancel.receiving.payload.request.CancelRecTransRequest
import com.kcvn.spm.app.cancel.receiving.payload.response.CancelRecTransResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Backlog
import com.kcvn.spm.model.tables.pojos.BacklogHistory
import com.kcvn.spm.model.tables.pojos.CancelReceivingTransactions
import com.kcvn.spm.repository.BacklogHistoryRepository
import com.kcvn.spm.repository.BacklogRepository
import com.kcvn.spm.repository.CancelReceivingTransactionsRepository
import com.kcvn.spm.repository.ReceivingTransactionsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CancelReceivingTransactionsService(
    private val cancelReceivingRepo: CancelReceivingTransactionsRepository,
    private val backlogRepo: BacklogRepository,
    private val backlogHistoryRepo: BacklogHistoryRepository,
    private val receivingRepo: ReceivingTransactionsRepository
) {
    fun createCancelReceiving(request: CancelRecTransRequest): CancelRecTransResponse? {
        val cancelRec = CancelReceivingTransactions(
            null,
            "KVC",
            request.locationCode,
            request.poNumber,
            request.qty,
            request.seqNo
        )
            receivingRepo.findRecTrans(cancelRec.destLocationCode!!, cancelRec.poNumber!!, cancelRec.qty!!, cancelRec.seqNo!!)
                ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        // save cancel receiving transactions
        val cancelRecId = cancelReceivingRepo.save(cancelRec)
        // minus backlog
        val backlog = backlogRepo.findByLocationCodeAndPO(request.locationCode!!, request.poNumber!!)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        val entityBacklog = Backlog(null, request.locationCode, request.poNumber, backlog.backlogQty?.minus(request.qty!!),
            backlog.boxQty?.minus(1)
        )
        backlogRepo.update(entityBacklog)
        // insert backlog history
        val entityBacklogHistory = BacklogHistory(
            null, entityBacklog.locationCode, entityBacklog.poNumber, entityBacklog.backlogQty, entityBacklog.boxQty, "CANCELED"
        )
        backlogHistoryRepo.save(entityBacklogHistory)
        return if (cancelRecId != null) {
            CancelRecTransResponse(
                cancelRec.destLocationCode!!,
                cancelRec.poNumber!!,
                cancelRec.qty!!,
                cancelRec.seqNo!!,
            )
        } else null
    }
}