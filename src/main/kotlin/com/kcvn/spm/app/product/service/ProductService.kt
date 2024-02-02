package com.kcvn.spm.app.product.service

import com.kcvn.spm.app.masterdata.service.MasterDataService
import com.kcvn.spm.app.product.dto.LayerImportProductModel
import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.product.payload.response.ProductResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.jsonhelper.JsonConvert
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.repository.CompletionRateProductRepository
import com.kcvn.spm.repository.ProductProcessRepository
import com.kcvn.spm.repository.ProductRepository
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jooq.tools.csv.CSVReader
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.InputStreamReader
import java.nio.charset.StandardCharsets

@Service
@Transactional
class ProductService(
    private val productRep: ProductRepository,
    private val productProcessRep: ProductProcessRepository,
    private val completionRateProductRep: CompletionRateProductRepository,
    private val masterDataService: MasterDataService
) {

    fun getListProduct(request: ProductSearchRequest?, pageable: Pageable) : PagingProductResponse {
        val products = productRep.getPagingList(request, pageable)
        var response = PagingProductResponse()

        if (products.first.isNotEmpty()) {
            response = mappingProductResponse(products.first)
            response.totalRecords = products.second
        }

        return response
    }

    fun importCsvProduct(file: MultipartFile): String {
        if (file.isEmpty) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val inputStream = file.inputStream
        val reader = CSVReader(InputStreamReader(inputStream, StandardCharsets.UTF_8))
        var data = reader.readAll()
        data = data.subList(1, data.size)
        if (data.isEmpty()) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productNames = data.mapNotNull { x -> x[0] }
        val productExists = productRep.getByName(productNames)

        val masterData = masterDataService.getMasterDataSelection()

        var count = 0

        for(item in data) {
            try {
                if (!masterData.exportTypeSelections.any { x -> x.value == item[1] }) continue
                if (!masterData.frame1Selections.any { x -> x.value == item[3] }) continue
                if (!masterData.frame2Selections.any { x -> x.value == item[4] }) continue
                if (!masterData.moldSelections.any { x -> x.value == item[5] }) continue
                if (!masterData.srNosrSelections.any { x -> x.value == item[7] }) continue
                if (!masterData.ringJigSelections.any { x -> x.value == item[11] }) continue
                if (!masterData.tapeCommonSelections.any { x -> x.value == item[14] }) continue
                if (!masterData.tapeTypeSelections.any { x -> x.value == item[15] }) continue

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
                }
                else {
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

    fun exportExcel(request: ProductSearchRequest?, pageable: Pageable) : BaseResponse<FileContentModel> {
        val products = productRep.getList(request, pageable)
        val productMapping = mappingProductResponse(products)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportProductTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (!productMapping.data.isNullOrEmpty()) {
            val style: CellStyle = workbook.createCellStyle()
            style.borderBottom = BorderStyle.THIN
            style.borderTop = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.wrapText = true

            val font: Font = workbook.createFont()
            font.fontName = "Times New Roman"
            font.fontHeightInPoints = 12.toShort()
            style.setFont(font)

            

            var rowNumber = 2
            for (item in productMapping.data!!) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                dataRow.createCell(0).setCellValue(item.name)
                dataRow.getCell(0).cellStyle = style

                dataRow.createCell(1).setCellValue(item.exportType)
                dataRow.getCell(1).cellStyle = style

                dataRow.createCell(2).setCellValue(item.size)
                dataRow.getCell(2).cellStyle = style

                dataRow.createCell(3).setCellValue(item.frame_1)
                dataRow.getCell(3).cellStyle = style

                dataRow.createCell(4).setCellValue(item.frame_2)
                dataRow.getCell(4).cellStyle = style

                dataRow.createCell(5).setCellValue(item.mold)
                dataRow.getCell(5).cellStyle = style

                dataRow.createCell(6).setCellValue(item.productLine)
                dataRow.getCell(6).cellStyle = style

                dataRow.createCell(7).setCellValue(item.srNosr)
                dataRow.getCell(7).cellStyle = style

                dataRow.createCell(8).setCellValue(item.pcsSh?.toString() ?: "")
                dataRow.getCell(8).cellStyle = style

                dataRow.createCell(9).setCellValue(item.shBlock?.toString() ?: "")
                dataRow.getCell(9).cellStyle = style

                dataRow.createCell(10).setCellValue(item.layerCount?.toString() ?: "")
                dataRow.getCell(10).cellStyle = style

                dataRow.createCell(11).setCellValue(item.completionRate?.toString() ?: "")
                dataRow.getCell(11).cellStyle = style
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "Danh_sach_san_pham.xlsx",
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        return BaseResponse(response)
    }

    private fun mappingProductResponse(products: List<Product>) : PagingProductResponse {
        val productNames = products.mapNotNull { x -> x.name }
        val completionRates = completionRateProductRep.getByProduct(productNames)
        val productProcesses = productProcessRep.getByProduct(productNames)
        val processGroups = productProcesses.groupBy { x -> Pair(x.productName, x.processCode) }

        val response = PagingProductResponse()
        response.data = products.map { x -> ProductResponse(
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

        response.columns = productProcesses.map { x -> DropdownResponse(x.processCode,x.processName) }.distinct()

        return response
    }
}