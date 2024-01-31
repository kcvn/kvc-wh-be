package com.kcvn.spm.app.product.service

import com.kcvn.spm.app.product.payload.request.ProductImportRequest
import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.product.payload.response.ProductResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.csvHelper.CsvMappingField
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.repository.CompletionRateProductRepository
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import com.opencsv.CSVReaderBuilder
import com.opencsv.bean.ColumnPositionMappingStrategy
import com.opencsv.bean.CsvToBeanBuilder
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

    fun importCsvProduct(file: MultipartFile) {
        if (file.isEmpty()) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val inputStream = file.inputStream
        val reader = CSVReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        val data = reader.readAll()
        if (data.isEmpty() || data.size == 1) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        for(i in 1 until data.size) {
            val item = data[i]
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

            product.productLayerDetail = ""
        }
    }
}