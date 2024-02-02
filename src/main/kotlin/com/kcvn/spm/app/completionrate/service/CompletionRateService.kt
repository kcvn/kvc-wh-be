package com.kcvn.spm.app.completionrate.service

import com.kcvn.spm.app.completionrate.dto.LayerImportCompletionRateProductModel
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessProductResponse
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessResponse
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProductResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProcess
import com.kcvn.spm.model.tables.pojos.CompletionRateProcessProduct
import com.kcvn.spm.model.tables.pojos.CompletionRateProduct
import com.kcvn.spm.repository.CompletionRateProcessProductRepository
import com.kcvn.spm.repository.CompletionRateProcessRepository
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
import java.time.LocalDateTime

@Service
@Transactional
class CompletionRateService(
    private val completionRateProductRepository: CompletionRateProductRepository,
    private val completionRateProcessProductRepository: CompletionRateProcessProductRepository,
    private val completionRateProcessRepository: CompletionRateProcessRepository
) {

    //Service Product
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

    fun importCsvCompletionRateProduct(file: MultipartFile): String {
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
                            key = (i - 2).toString()
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
                            key = (i - 2).toString()
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

    // Service Process
    fun importCsvProcess(file: MultipartFile, effectiveDate: LocalDateTime, expirationDate: LocalDateTime?): String {
        if (file.isEmpty()) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val inputStream = file.inputStream
        val reader = CSVReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var data = reader.readAll()
        data = data.subList(1, data.size)
        if (data.isEmpty() || data.size == 0) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productKeys = data.mapNotNull { x -> x[0] }

        val productExists = completionRateProcessRepository.getListCompletionRateProcessByKey(productKeys)

        var count = 0

        for(item in data) {
            try {
                val productExist = productExists.find { x -> x.key == item[0] }

                if (productExist == null) {
                    val compleRateProduct = CompletionRateProcess(
                        key = item[0],
                        rate =  BigDecimal(item[1]),
                        processCode = item[0].toString().take(6),
                        layerCode = item[0].toString().substring(6, 7),
                        expirationDate = expirationDate,
                        effectiveDate = effectiveDate
                    )

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            key = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO

                        }
                        layers.add(layer)
                    }
                    completionRateProcessRepository.add(compleRateProduct)
                } else {
                    productExist.key = item[0]
                    productExist.rate =  BigDecimal(item[1])

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            key = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO
                        }
                    }
                    completionRateProcessRepository.update(productExist)
                }

                count++
            } catch (e: BusinessException) {
                e.printStackTrace()
            }
        }

        return CommonUtils.getMessage("import.success", arrayOf(count, data.size))
    }
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


    //Service Process Product
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

    fun importCsvProcessProduct(file: MultipartFile, effectiveDate: LocalDateTime, expirationDate: LocalDateTime?): String {
        if (file.isEmpty()) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val inputStream = file.inputStream
        val reader = CSVReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var data = reader.readAll()
        data = data.subList(1, data.size)
        if (data.isEmpty() || data.size == 0) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productKeys = data.mapNotNull { x -> x[0] }

        val productExists = completionRateProcessProductRepository.getListProductByKey(productKeys)

        var count = 0

        for(item in data) {
            try {
                val productExist = productExists.find { x -> x.key == item[0] }

                if (productExist == null) {
                    val compleRateProduct = CompletionRateProcessProduct(
                        key = item[0],
                        rate =  BigDecimal(item[1]),
                        productNameShortcut = item[0].toString().take(7),
                        processCode = item[0].substring(7, 13),
                        layerCode = item[0].substring(13, 14),
                        expirationDate = expirationDate,
                        effectiveDate = effectiveDate
                    )

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            key = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO

                        }
                        layers.add(layer)
                    }
                    completionRateProcessProductRepository.add(compleRateProduct)
                } else {
                    productExist.key = item[0]
                    productExist.rate =  BigDecimal(item[1])

                    val layers = mutableListOf<LayerImportCompletionRateProductModel>()
                    for (i in 2 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportCompletionRateProductModel().apply {
                            key = (i - 2).toString()
                            rate = item[i]?.toBigInteger() ?: BigInteger.ZERO
                        }
                    }
                    completionRateProcessProductRepository.update(productExist)
                }

                count++
            } catch (e: BusinessException) {
                e.printStackTrace()
            }
        }

        return CommonUtils.getMessage("import.success", arrayOf(count, data.size))
    }
}