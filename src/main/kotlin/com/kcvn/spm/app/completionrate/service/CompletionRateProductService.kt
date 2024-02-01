package com.kcvn.spm.app.completionrate.service

import com.kcvn.spm.app.completionrate.dto.LayerImportCompletionRateProductModel
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProductResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProduct
import com.kcvn.spm.repository.CompletionRateProductRepository
import org.jooq.tools.csv.CSVReader
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.InputStreamReader
import java.math.BigDecimal
import java.math.BigInteger
import java.nio.charset.StandardCharsets

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

    fun importCsvProduct(file: MultipartFile): String {
        if (file.isEmpty()) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val inputStream = file.inputStream
        val reader = CSVReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var data = reader.readAll()
        data = data.subList(1, data.size)
        if (data.isEmpty() || data.size == 0) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productNames = data.mapNotNull { x -> x[0] }

        //wong here
        val productExists = completionRateProductRepository.getByProduct(productNames)

        var count = 0

        for(item in data) {
            try {
                val productExist = productExists.find { x -> x.productName == item[0] }

                if (productExist == null) {
                    val compleRateProduct = CompletionRateProduct(
                        productName = item[0],
                        rate =  BigDecimal(item[1])
                    )

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            productName = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO
                        }
                        layers.add(layer)
                    }
                    completionRateProductRepository.add(compleRateProduct)
                } else {
                    productExist.productName = item[0]
                    productExist.rate =  BigDecimal(item[1])

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            productName = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO
                        }
                    }
                    completionRateProductRepository.update(productExist)
                }

                count++
            } catch (e: BusinessException) {
                e.printStackTrace()
            }
        }

        return CommonUtils.getMessage("import.success", arrayOf(count, data.size))
    }
}