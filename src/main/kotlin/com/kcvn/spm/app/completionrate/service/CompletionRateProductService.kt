package com.kcvn.spm.app.completionrate.service

import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProductResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.repository.CompletionRateProductRepository
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompletionRateProductService(private val completionRateProductRepository: CompletionRateProductRepository) {
    fun getPaginatedCompletionRateProduct(search: String?, pageable: Pageable?): PaginatedResponse {
        val result = completionRateProductRepository.getPaginatedCompletionRateProduct(search, pageable)
        return PaginatedResponse(
            data = result.first.map { item ->
                CompletionRateProductResponse(
                    id = item.id,
                    productName = item.productName,
                    rate = item.rate,

                    )

            }, result.second
        )
    }
}