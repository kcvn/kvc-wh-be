package com.kcvn.spm.app.cancel.moving.service

import com.kcvn.spm.app.backlogwh.service.BacklogWhService
import com.kcvn.spm.app.cancel.moving.payload.request.CancelMovingRequest
import com.kcvn.spm.app.cancel.moving.payload.response.CancelMovingResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.*
import com.kcvn.spm.repository.*
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CancelMovingService(
    private val cancelMovingRepo: CancelMovingRepository,
    private val sendingRepo: SendingTransactionsRepository,
    private val backlogWhService: BacklogWhService,
    private val cancelReceivingRepo: CancelReceivingTransactionsRepository,
    private val receivingRepo: ReceivingTransactionsRepository,
    private val splittingRepo: SplittingRepository
) {
    fun createCancelMoving(request: CancelMovingRequest): CancelMovingResponse? {
        val cancelMoving = CancelSendingTransactions(
            null,
            request.sourceLocationCode,
            request.destLocationCode,
            request.sourcePackageCode,
            request.destPackageCode,
            request.poNumber,
            request.qty,
            request.seqNo,
            request.receivingSeqNo
        )
        sendingRepo.findMoving(cancelMoving.sourceLocationCode!!, cancelMoving.destLocationCode!!, cancelMoving.sourcePackageCode!!, cancelMoving.destPackageCode!!, cancelMoving.poNumber!!, cancelMoving.qty!!, cancelMoving.seqNo!!)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        // save cancel sending when moving
        val cancelMovingId = cancelMovingRepo.save(cancelMoving)
        // save cancel receiving transaction
        val cancelRec = CancelReceivingTransactions(
            null,
            request.sourceLocationCode,
            request.destLocationCode,
            request.sourcePackageCode,
            request.destPackageCode,
            request.poNumber,
            request.qty,
            request.receivingSeqNo
        )
        cancelReceivingRepo.save(cancelRec)
        // update is_canceled when moving
        val moving = SendingTransactions(
            null,
            request.sourceLocationCode,
            request.destLocationCode,
            request.sourcePackageCode,
            request.destPackageCode,
            request.poNumber,
            request.qty,
            request.seqNo,
            "TRANSFER"
        )
        sendingRepo.updateIsCanceled(moving)
        // update is_canceled in receiving transactions
        val rec = ReceivingTransactions(
            null,
            request.sourceLocationCode,
            request.destLocationCode,
            request.sourcePackageCode,
            request.destPackageCode,
            request.poNumber,
            request.qty,
            request.receivingSeqNo
        )
        receivingRepo.updateIsCanceled(rec)
        // plus backlog sourceLocation
        val backlogSourceData = BacklogWh(
            null,
            cancelMoving.sourceLocationCode,
            cancelMoving.poNumber,
            cancelMoving.sourcePackageCode,
            cancelMoving.qty,
            null
        )
        backlogWhService.plusBacklog(backlogSourceData, "CANCEL_OUT")
        // get receivingDate
        val splittingSource = splittingRepo.findByLocationAndPackage(cancelMoving.destLocationCode!!, cancelMoving.destPackageCode!!)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        val receivingDate = splittingSource.receivingDate
        // minus backlog destLocation
        val backlogDestData = BacklogWh(
            null,
            cancelMoving.destLocationCode,
            cancelMoving.poNumber,
            cancelMoving.destPackageCode,
            cancelMoving.qty,
            1,
            receivingDate
        )
        backlogWhService.minusBacklog(backlogDestData, "CANCEL_IN")

        return if (cancelMovingId != null) {
            CancelMovingResponse(
                cancelMoving.sourceLocationCode!!,
                cancelMoving.destLocationCode!!,
                cancelMoving.sourcePackageCode!!,
                cancelMoving.destPackageCode!!,
                cancelMoving.poNumber!!,
                cancelMoving.qty!!,
                cancelMoving.seqNo!!,
            )
        } else null
    }
}