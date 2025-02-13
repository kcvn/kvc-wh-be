package com.kcvn.spm.app.backlog.service

import com.kcvn.spm.app.backlog.payload.response.BacklogResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.repository.BacklogRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class BacklogService(private val backlogRepo: BacklogRepository) {
    fun findAllPaginated(search: String?, pageable: Pageable): PaginatedResponse {
        val result = backlogRepo.findByKeywordPaginated(search, pageable)
        return PaginatedResponse(
            result.first.map { BacklogResponse(it.locationCode!!, it.poNumber!!, it.backlogQty!!, it.boxQty!!) },
            result.second
        )
    }
}