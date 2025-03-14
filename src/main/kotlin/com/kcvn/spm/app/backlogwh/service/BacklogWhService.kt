package com.kcvn.spm.app.backlogwh.service

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.backlogwh.payload.response.BacklogWhResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.BacklogWhHistory
import com.kcvn.spm.repository.BacklogWhHistoryRepository
import com.kcvn.spm.repository.BacklogWhRepository
import com.kcvn.spm.repository.SplittingRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BacklogWhService(
    private val backlogWhRepo: BacklogWhRepository,
    private val backlogWhHistoryRepo: BacklogWhHistoryRepository,
    private val splittingRepo: SplittingRepository
) {
    fun plusBacklog(data: BacklogWh, transactionType: String) {
        val backlog = backlogWhRepo.findByLocationAndPackageAndPO(data.locationCode!!, data.packageCode!!, data.poNumber!!)
        // get receiving date
        val splitting = splittingRepo.findByLocationAndPackage(data.locationCode!!, data.packageCode!!)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        val receivingDate = splitting.receivingDate
        if (backlog == null) {
            val entityBacklog = BacklogWh(null, data.locationCode, data.poNumber, data.packageCode, data.backlogQty, data.boxQty, receivingDate)
            backlogWhRepo.save(entityBacklog)
            val entityBacklogHistory = BacklogWhHistory(
                null, data.locationCode, data.poNumber, data.packageCode, data.backlogQty, data.boxQty, receivingDate, null, transactionType
            )
            backlogWhHistoryRepo.save(entityBacklogHistory)
        } else {
            val entityBacklog = BacklogWh(null, data.locationCode, data.poNumber, data.packageCode, data.backlogQty?.plus(backlog.backlogQty!!),
                data.boxQty?.plus(backlog.boxQty!!)
            )
            backlogWhRepo.update(entityBacklog)
            val entityBacklogHistory = BacklogWhHistory(
                null, data.locationCode, data.poNumber, data.packageCode, data.backlogQty?.plus(backlog.backlogQty!!), data.boxQty?.plus(backlog.boxQty!!), receivingDate, null, transactionType
            )
            backlogWhHistoryRepo.save(entityBacklogHistory)
        }
    }

    fun minusBacklog(data: BacklogWh, transactionType: String) {
        val backlog = backlogWhRepo.findByLocationAndPackageAndPO(data.locationCode!!, data.packageCode!!, data.poNumber!!)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        val entityBacklog = BacklogWh(null, data.locationCode, data.poNumber, data.packageCode, backlog.backlogQty?.minus(data.backlogQty!!),
            backlog.boxQty?.minus(data.boxQty!!)
        )
        backlogWhRepo.update(entityBacklog)
        // insert backlog history
        val entityBacklogHistory = BacklogWhHistory(
            null, entityBacklog.locationCode, entityBacklog.poNumber, entityBacklog.packageCode, entityBacklog.backlogQty, entityBacklog.boxQty, data.receivingDate, null, transactionType
        )
        backlogWhHistoryRepo.save(entityBacklogHistory)
    }

    fun getByLocationAndPackageAndPO(locationCode: String, packageCode: String, poNumber: String): BacklogWh {
        return backlogWhRepo.findByLocationAndPackageAndPO(locationCode, packageCode, poNumber)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
    }

    fun getList(request: BacklogWhSearchRequest, pageable: Pageable): BasePagingResponse<BacklogWhResponse> {
        val backlogData = backlogWhRepo.getList(request, pageable)
        val data = backlogData.first.map {
            BacklogWhResponse(
                locationCode = it.locationCode,
                poNumber = it.poNumber,
                packageCode = it.packageCode,
                backlogQty = it.backlogQty,
                boxQty = it.boxQty,
                receivingDate = it.receivingDate,
                issueDate = it.issueDate
            )
        }
        return BasePagingResponse(
            data,
            backlogData.second
        )
    }
}