package com.kcvn.spm.app.cancel.sending.service

import com.kcvn.spm.app.backlog.service.BacklogService
import com.kcvn.spm.app.cancel.sending.payload.request.CancelSendTransRequest
import com.kcvn.spm.app.cancel.sending.payload.response.CancelSendTransResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Backlog
import com.kcvn.spm.model.tables.pojos.CancelSendingTransactions
import com.kcvn.spm.model.tables.pojos.SendingTransactions
import com.kcvn.spm.repository.CancelSendingTransactionsRepository
import com.kcvn.spm.repository.SendingTransactionsRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CancelSendingTransactionsService(
    private val cancelSendingRepo: CancelSendingTransactionsRepository,
    private val sendingRepo: SendingTransactionsRepository,
    private val backlogService: BacklogService
) {
    fun createCancelSending(request: CancelSendTransRequest): CancelSendTransResponse? {
        val cancelSend = CancelSendingTransactions(
            null,
            request.locationCode,
            "KVC",
            request.poNumber,
            request.qty,
            request.seqNo
        )
        sendingRepo.findMoving(cancelSend.sourceLocationCode!!, cancelSend.destLocationCode!!, cancelSend.poNumber!!, cancelSend.qty!!, cancelSend.seqNo!!, null)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        // save cancel sending transactions
        val cancelSendId = cancelSendingRepo.save(cancelSend)
        // update is_canceled in sending transactions
        val send = SendingTransactions(
            null,
            request.locationCode,
            "KVC",
            request.poNumber,
            request.qty,
            request.seqNo
        )
        sendingRepo.updateIsCanceled(send)
        // plus backlog
        val backlogData = Backlog(
            null,
            cancelSend.sourceLocationCode,
            cancelSend.poNumber,
            cancelSend.qty,
            null
        )
        backlogService.plusBacklog(backlogData, "CANCEL_OUT_ONLY")

        return if (cancelSendId != null) {
            CancelSendTransResponse(
                cancelSend.sourceLocationCode!!,
                cancelSend.poNumber!!,
                cancelSend.qty!!,
                cancelSend.seqNo!!,
            )
        } else null
    }
}