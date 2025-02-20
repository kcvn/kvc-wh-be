package com.kcvn.spm.app.cancel.moving.service

import com.kcvn.spm.app.backlog.service.BacklogService
import com.kcvn.spm.app.cancel.moving.payload.request.CancelMovingRequest
import com.kcvn.spm.app.cancel.moving.payload.response.CancelMovingResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Backlog
import com.kcvn.spm.model.tables.pojos.CancelMoving
import com.kcvn.spm.repository.CancelMovingRepository
import com.kcvn.spm.repository.MovingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CancelMovingService(
    private val cancelMovingRepo: CancelMovingRepository,
    private val movingRepo: MovingRepository,
    private val backlogService: BacklogService
) {
    fun createCancelMoving(request: CancelMovingRequest): CancelMovingResponse? {
        val cancelMoving = CancelMoving(
            null,
            request.sourceLocationCode,
            request.destLocationCode,
            request.poNumber,
            request.qty,
            request.seqNo
        )
        movingRepo.findMoving(cancelMoving.sourceLocationCode!!, cancelMoving.destLocationCode!!, cancelMoving.poNumber!!, cancelMoving.qty!!, cancelMoving.seqNo!!)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        // save cancel moving
        val cancelMovingId = cancelMovingRepo.save(cancelMoving)
        // plus backlog sourceLocation
        val backlogSourceData = Backlog(
            null,
            cancelMoving.sourceLocationCode,
            cancelMoving.poNumber,
            cancelMoving.qty,
            null
        )
        backlogService.plusBacklog(backlogSourceData, "CANCEL_OUT")
        // minus backlog destLocation
        val backlogDestData = Backlog(
            null,
            cancelMoving.destLocationCode,
            cancelMoving.poNumber,
            cancelMoving.qty,
            null
        )
        backlogService.minusBacklog(backlogDestData, "CANCEL_IN")

        return if (cancelMovingId != null) {
            CancelMovingResponse(
                cancelMoving.sourceLocationCode!!,
                cancelMoving.destLocationCode!!,
                cancelMoving.poNumber!!,
                cancelMoving.qty!!,
                cancelMoving.seqNo!!,
            )
        } else null
    }
}