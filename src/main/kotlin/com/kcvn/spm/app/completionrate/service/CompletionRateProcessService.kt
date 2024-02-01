package com.kcvn.spm.app.completionrate.service

import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.repository.CompletionRateProcessRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompletionRateProcessService(private val completionRateProcessRepository: CompletionRateProcessRepository) {

    fun getPaginatedCompletionRateProcesses(search: String?, pageable: Pageable?): PaginatedResponse {
        val result = completionRateProcessRepository.getPaginatedCompletionRateProcesses(search, pageable)
        return PaginatedResponse(
            data = result.first.map { item ->
                CompletionRateProcessResponse(
                    id = item.id,
                    key = item.key,
                    processCode = item.processCode,
                    layerCode = item.layerCode,
                    rate = item.rate,

                    )
            }, result.second
        )
    }
}
