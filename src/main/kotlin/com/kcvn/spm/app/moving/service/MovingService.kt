package com.kcvn.spm.app.moving.service

import com.kcvn.spm.app.backlog.service.BacklogService
import com.kcvn.spm.app.moving.payload.MovingRequest
import com.kcvn.spm.app.moving.payload.MovingRequestWithSeq
import com.kcvn.spm.model.tables.pojos.Backlog
import com.kcvn.spm.model.tables.pojos.Moving
import com.kcvn.spm.repository.MovingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class MovingService(
    private val movingRepo: MovingRepository,
    private val backlogService: BacklogService
) {
    fun saveMoving(request: List<MovingRequest>) {
        val list = createMovingRequestWithSeq(request)
        list.forEach {
            val moving = Moving(
                null,
                it.sourceLocationCode,
                it.destLocationCode,
                it.poNumber,
                it.qty,
                it.seqNo
            )
            // save moving
            movingRepo.save(moving)
            // plus backlog destLocation
            val backlogDestData = Backlog(
                null,
                it.destLocationCode,
                it.poNumber,
                it.qty,
                null
            )
            backlogService.plusBacklog(backlogDestData, "IN")
            // minus backlog sourceLocation
            val backlogSourceData = Backlog(
                null,
                it.sourceLocationCode,
                it.poNumber,
                it.qty,
                null
            )
            backlogService.minusBacklog(backlogSourceData, "OUT")
        }
    }

    fun createMovingRequestWithSeq(requests: List<MovingRequest>): List<MovingRequestWithSeq> {
        return requests
            .groupBy { Triple(it.sourceLocationCode, it.destLocationCode, it.poNumber) }
            .flatMap { (key, group) ->
                val (sourceLocationCode, destLocationCode, poNumber) = key
                val latestSeqNo = movingRepo.findLatestMoving(sourceLocationCode!!, destLocationCode!!, poNumber!!)?.seqNo ?: 0

                group.mapIndexed { index, movingRequest ->
                    MovingRequestWithSeq(
                        sourceLocationCode = movingRequest.sourceLocationCode,
                        destLocationCode = movingRequest.destLocationCode,
                        poNumber = movingRequest.poNumber,
                        qty = movingRequest.qty,
                        seqNo = latestSeqNo + index + 1 // Bắt đầu từ latestSeqNo + 1, tăng dần
                    )
                }
            }
    }
}