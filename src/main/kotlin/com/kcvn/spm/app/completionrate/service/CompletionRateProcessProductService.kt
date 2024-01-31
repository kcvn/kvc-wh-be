package com.kcvn.spm.app.completionrate.service
import com.kcvn.spm.repository.CompletionRateProcessProductRepository
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessProductResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class CompletionRateProcessProductService(private val completionRateProcessProductRepository: CompletionRateProcessProductRepository) {

    fun getPaginatedCompletionRateProcessesProduct(search: String?, pageable: Pageable?): PaginatedResponse {
        val result = completionRateProcessProductRepository.getPaginatedCompletionRateProcessesProduct(search, pageable)
        return PaginatedResponse(
                data = result.first.map { item ->
                    CompletionRateProcessProductResponse(
                        id = item.id,
                        key = item.key,
                        productNameShortcut = item.productNameShortcut,
                        processCode = item.processCode,
                        layerCode = item.layerCode,
                        rate = item.rate

                    )
                }, result.second
        )
    }
}
