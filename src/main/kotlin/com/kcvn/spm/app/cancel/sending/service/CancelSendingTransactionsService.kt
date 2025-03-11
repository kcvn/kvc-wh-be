package com.kcvn.spm.app.cancel.sending.service

import com.kcvn.spm.app.backlogwh.service.BacklogWhService
import com.kcvn.spm.app.cancel.sending.payload.request.CancelSendTransRequest
import com.kcvn.spm.app.cancel.sending.payload.response.CancelSendTransResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
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
    private val backlogWhService: BacklogWhService
) {
    fun createCancelSending(request: CancelSendTransRequest): CancelSendTransResponse? {
        val cancelSend = CancelSendingTransactions(
            null,
            request.locationCode,
            "KVC",
            request.packageCode,
            request.packageCode,
            request.poNumber,
            request.qty,
            request.seqNo
        )
        sendingRepo.findMoving(cancelSend.sourceLocationCode!!, cancelSend.destLocationCode!!, cancelSend.sourcePackageCode!!, cancelSend.destPackageCode!!, cancelSend.poNumber!!, cancelSend.qty!!, cancelSend.seqNo!!, null)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        // save cancel sending transactions
        val cancelSendId = cancelSendingRepo.save(cancelSend)
        // update is_canceled in sending transactions
        val send = SendingTransactions(
            null,
            request.locationCode,
            "KVC",
            request.packageCode,
            request.packageCode,
            request.poNumber,
            request.qty,
            request.seqNo
        )
        sendingRepo.updateIsCanceled(send)
        // plus backlog
        val backlogData = BacklogWh(
            null,
            cancelSend.sourceLocationCode,
            cancelSend.poNumber,
            cancelSend.sourcePackageCode,
            cancelSend.qty,
            null
        )
        backlogWhService.plusBacklog(backlogData, "CANCEL_OUT_ONLY")

        return if (cancelSendId != null) {
            CancelSendTransResponse(
                cancelSend.sourceLocationCode!!,
                cancelSend.sourcePackageCode,
                cancelSend.poNumber!!,
                cancelSend.qty!!,
                cancelSend.seqNo!!,
            )
        } else null
    }
}