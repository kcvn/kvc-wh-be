package com.kcvn.spm.app.cancel.receiving.service

import com.kcvn.spm.app.backlog.service.BacklogService
import com.kcvn.spm.app.cancel.receiving.payload.request.CancelRecTransRequest
import com.kcvn.spm.app.cancel.receiving.payload.response.CancelRecTransResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Backlog
import com.kcvn.spm.model.tables.pojos.CancelReceivingTransactions
import com.kcvn.spm.model.tables.pojos.ReceivingTransactions
import com.kcvn.spm.repository.CancelReceivingTransactionsRepository
import com.kcvn.spm.repository.ReceivingTransactionsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CancelReceivingTransactionsService(
    private val cancelReceivingRepo: CancelReceivingTransactionsRepository,
    private val receivingRepo: ReceivingTransactionsRepository,
    private val backlogService: BacklogService
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
        // update is_canceled in receiving transactions
        val rec = ReceivingTransactions(
            null,
            "KVC",
            request.locationCode,
            request.poNumber,
            request.qty,
            request.seqNo
        )
        receivingRepo.updateIsCanceled(rec)
        // minus backlog
        val backlogData = Backlog(
            null,
            cancelRec.destLocationCode,
            cancelRec.poNumber,
            cancelRec.qty,
            null
        )
        backlogService.minusBacklog(backlogData, "CANCEL_IN_ONLY")

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