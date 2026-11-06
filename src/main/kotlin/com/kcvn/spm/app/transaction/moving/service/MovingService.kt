package com.kcvn.spm.app.transaction.moving.service

import com.kcvn.spm.app.backlogwh.service.BacklogWhService
import com.kcvn.spm.app.transaction.moving.payload.request.MovingRequest
import com.kcvn.spm.app.transaction.moving.payload.request.MovingRequestWithSeq
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.exception.BusinessExceptionDetail
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.Moving
import com.kcvn.spm.model.tables.pojos.Splitting
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

        list.forEach { request ->
            val sourceData = splittingRepo.findByPackage(request.sourcePackageCode!!)
            if (sourceData != null) {
                splittingRepo.delete(sourceData)
                val destData = sourceData.copy(
                    id = null,
                    locationCode = request.destLocationCode,
                    isSyncedAmoeba = false,
                    isDeleted = false,
                )
                splittingRepo.save(destData)
            }
            else throw BusinessException("${request.sourcePackageCode} không tồn tại")
            // get receivingDate from source
            val sourceBacklog = backlogWhRepo.findByPackageCode(request.sourcePackageCode!!)
                ?: throw BusinessExceptionDetail(
                    CommonUtils.getMessage("data.not.found.in.backlog"), "packageCode = ${request.sourcePackageCode}")
            /*val splittingSource = splittingRepo.findByLocationAndPackage(it.sourceLocationCode!!, it.sourcePackageCode!!)
                ?: throw BusinessExceptionDetail(
                    CommonUtils.getMessage("data.not.found.in.splitting"), "locationCode = ${it.sourceLocationCode}, packageCode = ${it.sourcePackageCode}"
                )*/
            sourceBacklog.forEach { moving ->
                val recDateSource = moving.receivingDate
                // save moving
                val moving = Moving(
                    null,
                    sourceData.locationCode,
                    request.destLocationCode,
                    request.sourcePackageCode,
                    request.sourcePackageCode,
                    moving.poNumber,
                    moving.backlogQty,
                    request.seqNo,
                    "TRANSFER",
                    recDateSource,
                    lotNo = moving.lotNo,
                    issueDate = moving.issueDate
                )
                val refId = movingRepo.saveMoving(moving)
            }

        }
    }

    fun createMovingRequestWithSeq(requests: List<MovingRequest>, todayUtc: LocalDate): List<MovingRequestWithSeq> {
        return requests
            .groupBy { (it.sourcePackageCode) }
            .flatMap { (key, group) ->
                val sourcePackageCode = key
                val latestSeqNo = movingRepo.findLatestMoving(sourcePackageCode!!, todayUtc)?.seqNo ?: 0

                group.mapIndexed { index, movingRequest ->
                    MovingRequestWithSeq(
                        destLocationCode = movingRequest.destLocationCode,
                        sourcePackageCode = movingRequest.sourcePackageCode,
                        seqNo = latestSeqNo + index + 1 // Bắt đầu từ latestSeqNo + 1, tăng dần
                    )
                }
            }
    }
}