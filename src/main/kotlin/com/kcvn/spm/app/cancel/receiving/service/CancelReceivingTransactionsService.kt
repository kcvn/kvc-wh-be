package com.kcvn.spm.app.cancel.receiving.service

import com.kcvn.spm.app.backlogwh.service.BacklogWhService
import com.kcvn.spm.app.cancel.receiving.payload.request.CancelRecTransRequest
import com.kcvn.spm.app.cancel.receiving.payload.response.CancelRecTransResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
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
    private val backlogWhService: BacklogWhService
) {
    fun createCancelReceiving(request: CancelRecTransRequest): CancelRecTransResponse? {
        val cancelRec = CancelReceivingTransactions(
            null,
            "KVC",
            request.locationCode,
            request.packageCode,
            request.packageCode,
            request.poNumber,
            request.qty,
            request.seqNo
        )
            receivingRepo.findRecTrans(cancelRec.destLocationCode!!, cancelRec.sourcePackageCode!!, cancelRec.poNumber!!, cancelRec.qty!!, cancelRec.seqNo!!)
                ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        // save cancel receiving transactions
        val cancelRecId = cancelReceivingRepo.save(cancelRec)
        // update is_canceled in receiving transactions
        val rec = ReceivingTransactions(
            null,
            "KVC",
            request.locationCode,
            request.packageCode,
            request.packageCode,
            request.poNumber,
            request.qty,
            request.seqNo
        )
        receivingRepo.updateIsCanceled(rec)
        // minus backlog
        val backlogData = BacklogWh(
            null,
            cancelRec.destLocationCode,
            cancelRec.poNumber,
            cancelRec.sourcePackageCode,
            cancelRec.qty,
            null
        )
        backlogWhService.minusBacklog(backlogData, "CANCEL_IN_ONLY")

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