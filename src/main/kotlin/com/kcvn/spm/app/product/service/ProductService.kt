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
import org.apache.poi.ss.usermodel.Cell
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.jooq.tools.csv.CSVReader
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
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
            response.columns = productProcesses.mapNotNull { x -> DropdownResponse(x.processCode,x.processName) }.distinct()
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

        val workbook = XSSFWorkbook()
        val sheet = workbook.createSheet("Data")

        val headerRow: Row = sheet.createRow(0)

//        for (i in 0 until 16) {
//            val cell: Cell = headerRow.createCell(i - 1)
//            cell.setCellValue(metaData.getColumnName(i))
//        }

        var rowNumber = 1
        for(item in products) {
            val dataRow: Row = sheet.createRow(rowNumber++)

            val cell: Cell = dataRow.createCell(0)
            cell.setCellValue(item.name)
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "Danh_sach_san_pham.xlsx",
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        return BaseResponse<FileContentModel>(response)
    }

    fun copySheet(sourceSheet: Sheet, targetSheet: Sheet) {
        for (rowNum in 0 until sourceSheet.physicalNumberOfRows) {
            val sourceRow = sourceSheet.getRow(rowNum)
            val targetRow = targetSheet.getRow(rowNum) ?: targetSheet.createRow(rowNum)

            for (cellNum in 0 until sourceRow.physicalNumberOfCells) {
                val sourceCell = sourceRow.getCell(cellNum)
                val targetCell = targetRow.createCell(cellNum)

                targetCell.cellStyle = sourceCell.cellStyle

                when (sourceCell.cellType) {
                    CellType.NUMERIC -> targetCell.setCellValue(sourceCell.numericCellValue)
                    CellType.STRING -> targetCell.setCellValue(sourceCell.stringCellValue)
                    CellType.BOOLEAN -> targetCell.setCellValue(sourceCell.booleanCellValue)
                    CellType.FORMULA -> targetCell.cellFormula = sourceCell.cellFormula
                    CellType.BLANK -> targetCell.setCellValue("")
                    CellType.ERROR -> targetCell.setCellValue("")
                    else -> targetCell.setCellValue("")
                }
            }
        }
    }
}