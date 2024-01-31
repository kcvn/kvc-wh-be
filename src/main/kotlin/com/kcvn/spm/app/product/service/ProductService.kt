package com.kcvn.spm.app.product.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.kcvn.spm.app.product.dto.LayerImportProductModel
import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.product.payload.response.ProductResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.jsonhelper.JsonConvert
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.repository.CompletionRateProductRepository
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import org.jooq.tools.csv.CSVReader
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Service
@Transactional
class ProductService(
    private val productRep: ProductRepository,
    private val productProcessRep: ProductProcessRepository,
    private val completionRateProductRep: CompletionRateProductRepository
) {

    fun getListProduct(request: ProductSearchRequest?, pageable: Pageable) : PagingProductResponse {
        val products = productRep.getList(request, pageable)
        val response = PagingProductResponse()

        if (products.first.isNotEmpty()) {
            val productNames = products.first.mapNotNull { x -> x.name }
            val completionRates = completionRateProductRep.getByProduct(productNames)
            val productProcesses = productProcessRep.getByProduct(productNames)
            val processGroups = productProcesses.groupBy { x -> Pair(x.productName, x.processCode) }

            response.data = products.first.map { x -> ProductResponse(
                id = x.id,
                name = x.name,
                exportType = x.exportType,
                size = x.size,
                frame_1 = x.frame_1,
                frame_2 = x.frame_2,
                mold = x.mold,
                productLine = x.productLine,
                srNosr = x.srNosr,
                pcsSh = x.pcsSh,
                shBlock = x.shBlock,
                layerCount = x.layerCount,
                ringJig = x.ringJig,
                snapMold = x.snapMold,
                tapeCommon = x.tapeCommon,
                tapeType = x.tapeType,
                completionRate = (completionRates.find { m -> m.productName == x.name }?.rate ?: 0.0).toDouble(),
                lstProcess = processGroups.filter { m -> m.key.first == x.name }.mapNotNull { m -> DropdownResponse(m.key.second, m.value.size.toString()) }
            ) }
            response.totalRecords = products.second
            response.collumns = productProcesses.mapNotNull { x -> DropdownResponse(x.processCode,x.processName) }.distinct()
        }

        return response
    }

    fun importCsvProduct(file: MultipartFile): String {
        if (file.isEmpty()) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val inputStream = file.inputStream
        val reader = CSVReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var data = reader.readAll()
        data = data.subList(1, data.size)
        if (data.isEmpty() || data.size == 0) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productNames = data.mapNotNull { x -> x[0] }
        val productExists = productRep.getByName(productNames)

        var count = 0

        for(item in data) {
            try {
                val productExist = productExists.find { x -> x.name == item[0] }

                if (productExist == null) {
                    val product = Product(
                        name = item[0],
                        exportType = item[1],
                        size = item[2],
                        frame_1 = item[3],
                        frame_2 = item[4],
                        mold = item[5],
                        productLine = item[6],
                        srNosr = item[7],
                        pcsSh = item[8]?.toInt() ?: 0,
                        shBlock = item[9]?.toInt() ?: 0,
                        layerCount = item[10]?.toInt() ?: 0,
                        ringJig = item[11],
                        process = item[12]?.toInt() ?: 0,
                        snapMold = item[13],
                        tapeCommon = item[14],
                        tapeType = item[15]
                    )

                    val layers = mutableListOf<LayerImportProductModel>()
                    for (i in 16 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportProductModel((i - 15).toString(), (item[i]?.toInt() ?: 0))
                        layers.add(layer)
                    }

                    product.productLayerDetail = JsonConvert.serialize(layers)

                    productRep.add(product)
                } else {
                    productExist.exportType = item[1]
                    productExist.size = item[2]
                    productExist.frame_1 = item[3]
                    productExist.frame_2 = item[4]
                    productExist.mold = item[5]
                    productExist.productLine = item[6]
                    productExist.srNosr = item[7]
                    productExist.pcsSh = item[8]?.toInt() ?: 0
                    productExist.shBlock = item[9]?.toInt() ?: 0
                    productExist.layerCount = item[10]?.toInt() ?: 0
                    productExist.ringJig = item[11]
                    productExist.process = item[12]?.toInt() ?: 0
                    productExist.snapMold = item[13]
                    productExist.tapeCommon = item[14]
                    productExist.tapeType = item[15]

                    val layers = mutableListOf<LayerImportProductModel>()
                    for (i in 16 until item.size) {
                        if (item[i].isNullOrEmpty()) continue
                        val layer = LayerImportProductModel((i - 15).toString(), (item[i]?.toInt() ?: 0))
                        layers.add(layer)
                    }

                    productExist.productLayerDetail = JsonConvert.serialize(layers)

                    productRep.update(productExist)
                }

                count++
            } catch (e: BusinessException) {
                e.printStackTrace()
            }
        }

        return CommonUtils.getMessage("import.success", arrayOf(count, data.size))
    }
    
    fun getProductDetail (request: String) : ProductResponse? {
        val query = productRep.getProductDetail(request)
        if (query == null)
        {
            return null
        }
        else
        {
            val data = ProductResponse(
                id = query.id,
                name = query.name,
                exportType = query.exportType,
                size = query.size,
                frame_1 = query.frame_1,
                frame_2 = query.frame_2,
                mold = query.mold,
                productLine = query.productLine,
                srNosr = query.srNosr,
                pcsSh = query.pcsSh,
                shBlock = query.shBlock,
                layerCount = query.layerCount,
                ringJig = query.ringJig,
                snapMold = query.snapMold,
                tapeCommon = query.tapeCommon,
                tapeType = query.tapeType,
                productLayerDetail = query.productLayerDetail,
                process = query.process
            )
            return data
        }
    }
}