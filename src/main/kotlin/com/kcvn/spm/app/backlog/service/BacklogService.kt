package com.kcvn.spm.app.backlog.service

import com.kcvn.spm.app.backlog.payload.request.BacklogSearchRequest
import com.kcvn.spm.app.backlog.payload.response.BacklogResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Backlog
import com.kcvn.spm.model.tables.pojos.BacklogHistory
import com.kcvn.spm.repository.BacklogHistoryRepository
import com.kcvn.spm.repository.BacklogRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BacklogService(
    private val backlogRepo: BacklogRepository,
    private val backlogHistoryRepo: BacklogHistoryRepository
) {
    fun getList(request: BacklogSearchRequest, pageable: Pageable): BasePagingResponse<BacklogResponse> {
        val backlogData = backlogRepo.getList(request, pageable)
        val data = backlogData.first.map {
            BacklogResponse(
                locationCode = it.locationCode,
                poNumber = it.poNumber,
                backlogQty = it.backlogQty,
                boxQty = it.boxQty
            )
        }
        return BasePagingResponse(
            data,
            backlogData.second
        )
    }

    fun plusBacklog(data: Backlog, transactionType: String) {
        val backlog = backlogRepo.findByLocationCodeAndPO(data.locationCode!!, data.poNumber!!)
        if (backlog == null) {
            val entityBacklog = Backlog(null, data.locationCode, data.poNumber, data.backlogQty, 1)
            backlogRepo.save(entityBacklog)
            val entityBacklogHistory = BacklogHistory(null, data.locationCode, data.poNumber, data.backlogQty, 1, transactionType)
            backlogHistoryRepo.save(entityBacklogHistory)
        } else {
            val entityBacklog = Backlog(null, data.locationCode, data.poNumber, data.backlogQty?.plus(backlog.backlogQty!!), 1.plus(backlog.boxQty!!))
            backlogRepo.update(entityBacklog)
            val entityBacklogHistory = BacklogHistory(
                null, data.locationCode, data.poNumber, data.backlogQty?.plus(backlog.backlogQty!!), 1.plus(backlog.boxQty!!), transactionType
            )
            backlogHistoryRepo.save(entityBacklogHistory)
        }
    }

    fun minusBacklog(data: Backlog, transactionType: String) {
        val backlog = backlogRepo.findByLocationCodeAndPO(data.locationCode!!, data.poNumber!!)
            ?: throw BusinessException(CommonUtils.getMessage("data.notFound"))
        val entityBacklog = Backlog(null, data.locationCode, data.poNumber, backlog.backlogQty?.minus(data.backlogQty!!),
            backlog.boxQty?.minus(1)
        )
        backlogRepo.update(entityBacklog)
        // insert backlog history
        val entityBacklogHistory = BacklogHistory(
            null, entityBacklog.locationCode, entityBacklog.poNumber, entityBacklog.backlogQty, entityBacklog.boxQty, transactionType
        )
        backlogHistoryRepo.save(entityBacklogHistory)
    }
}