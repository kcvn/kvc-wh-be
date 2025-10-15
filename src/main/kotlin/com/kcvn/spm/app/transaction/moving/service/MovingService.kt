package com.kcvn.spm.app.transaction.moving.service

import com.kcvn.spm.app.backlogwh.service.BacklogWhService
import com.kcvn.spm.app.transaction.moving.payload.request.MovingRequest
import com.kcvn.spm.app.transaction.moving.payload.request.MovingRequestWithSeq
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.Moving
import com.kcvn.spm.repository.BacklogWhRepository
import com.kcvn.spm.repository.MovingRepository
import com.kcvn.spm.repository.SplittingRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Service
@Transactional
class MovingService(
    private val movingRepo: MovingRepository,
    private val backlogWhService: BacklogWhService,
    private val splittingRepo: SplittingRepository,
    private val backlogWhRepo: BacklogWhRepository
) {
    fun saveMoving(request: List<MovingRequest>) {
        val todayUtc = OffsetDateTime.now(ZoneOffset.UTC).toLocalDate()
        val list = createMovingRequestWithSeq(request, todayUtc)

        list.forEach {
            // get receivingDate from source
            val sourceBacklog = backlogWhRepo.findByLocationAndPackageAndPO(it.sourceLocationCode!!, it.sourcePackageCode!!, it.poNumber!!)
            val splittingSource = splittingRepo.findByLocationAndPackage(it.sourceLocationCode!!, it.sourcePackageCode!!)
                ?: throw BusinessExceptionDetail(
                    CommonUtils.getMessage("data.not.found.in.splitting"), "locationCode = ${it.sourceLocationCode}, packageCode = ${it.sourcePackageCode}"
                )
            val recDateSource = splittingSource.receivingDate
            // save moving
            val moving = Moving(
                null,
                it.sourceLocationCode,
                it.destLocationCode,
                it.sourcePackageCode,
                it.destPackageCode,
                it.poNumber,
                it.qty,
                it.seqNo,
                "TRANSFER",
                recDateSource
            )
            movingRepo.saveMoving(moving)
            // update location for package when destPackageCode == ""
            if (it.destPackageCode == "") {
                splittingRepo.updateLocationCode(it.sourceLocationCode!!, it.destLocationCode!!, it.sourcePackageCode!!)
            }
            // plus backlog destLocation
            val backlogDestData = BacklogWh(
                null,
                it.destLocationCode,
                it.poNumber,
                if (it.destPackageCode?.isNotEmpty() == true) it.destPackageCode else it.sourcePackageCode,
                it.qty,
                it.boxQty,
                isEntried = false,
                inspectionDate = sourceBacklog?.inspectionDate
            )
            backlogWhService.plusBacklog(backlogDestData, "IN")
            // minus backlog sourceLocation
            val backlogSourceData = BacklogWh(
                null,
                it.sourceLocationCode,
                it.poNumber,
                it.sourcePackageCode,
                it.qty,
                it.boxQty,
                recDateSource,
                isEntried = sourceBacklog?.isEntried,
                inspectionDate = sourceBacklog?.inspectionDate
            )
            backlogWhService.minusBacklog(backlogSourceData, "OUT")
            backlogWhRepo.updateIsEntried(false, sourceBacklog?.poNumber!!, recDateSource!!, sourceBacklog.inspectionDate)
        }
    }

    fun createMovingRequestWithSeq(requests: List<MovingRequest>, todayUtc: LocalDate): List<MovingRequestWithSeq> {
        return requests
            .groupBy { Triple(it.sourceLocationCode, it.sourcePackageCode, it.poNumber) }
            .flatMap { (key, group) ->
                val (sourceLocationCode, sourcePackageCode, poNumber) = key
                val latestSeqNo = movingRepo.findLatestMoving(sourceLocationCode!!, sourcePackageCode!!, poNumber!!, todayUtc)?.seqNo ?: 0

                group.mapIndexed { index, movingRequest ->
                    MovingRequestWithSeq(
                        destLocationCode = movingRequest.destLocationCode,
                        destPackageCode = movingRequest.destPackageCode,
                        sourceLocationCode = movingRequest.sourceLocationCode,
                        sourcePackageCode = movingRequest.sourcePackageCode,
                        poNumber = movingRequest.poNumber,
                        qty = movingRequest.qty,
                        boxQty = movingRequest.boxQty,
                        seqNo = latestSeqNo + index + 1 // Bắt đầu từ latestSeqNo + 1, tăng dần
                    )
                }
            }
    }
}