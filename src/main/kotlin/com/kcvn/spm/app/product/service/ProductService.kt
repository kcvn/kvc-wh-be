package com.kcvn.spm.app.product.service

import com.kcvn.spm.app.masterdata.payload.response.MasterDataSelectionResponse
import com.kcvn.spm.app.masterdata.service.MasterDataService
import com.kcvn.spm.app.product.payload.model.LayerImportProductModel
import com.kcvn.spm.app.product.payload.model.ProductModel
import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.excelhelper.ExcelHelper
import com.kcvn.spm.common.helper.jsonhelper.JsonConvert
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.repository.CompletionRateProductRepository
import com.kcvn.spm.repository.ProcessProcedureStructureRepository
import com.kcvn.spm.repository.ProductRepository
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ProductService(
    private val productRep: ProductRepository,
    private val processProcedureStructureRep: ProcessProcedureStructureRepository,
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

    fun getProductDetail(request: String?): Product? {
        return productRep.getProductDetail(request)
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
            font.fontName = Constants.FONT_TIMES_NEW_ROMAN
            font.fontHeightInPoints = 12.toShort()
            style.setFont(font)

            if (!productMapping.columns.isNullOrEmpty()) {
                var headerCol = 12
                val headerRow: Row = sheet.getRow(1)
                val headerStyle = headerRow.getCell(11).cellStyle
                for (col in productMapping.columns!!) {
                    headerRow.createCell(headerCol).setCellValue(col.value)
                    headerRow.getCell(headerCol).cellStyle = headerStyle
                    headerCol++
                }
            }

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

                if (!productMapping.columns.isNullOrEmpty()) {
                    var cellIndex = 12
                    for (col in productMapping.columns!!) {
                        val cellValue = item.lstProcess.find { x -> x.key == col.key }
                        dataRow.createCell(cellIndex).setCellValue(cellValue?.value ?: "")
                        dataRow.getCell(cellIndex).cellStyle = style
                        cellIndex++
                    }
                }
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportListProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = Constants.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun downloadTemplate() : BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProductTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importProductTemplate"),
            contentType = Constants.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }

    fun importExcelProduct(file: MultipartFile) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))
        if (ExcelHelper.fileIsEmpty(sheet, rowIndex)) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = productRep.getByName(productNames)
        val masterData = masterDataService.getMasterDataSelection()
        var count = 0
        var total = 0

        val headerRow = sheet.getRow(0)
        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProductTemplate.xlsx"

        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 16))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

        val colEmpty = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == "" }
        val colResult = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == CommonUtils.getMessage("excel.colResultName") }
        val colIndexResult = colResult?.columnIndex ?: (colEmpty?.columnIndex ?: (sheet.first().lastCellNum + 0))

        if (colResult == null) {
            headerRow.createCell(colIndexResult).setCellValue(CommonUtils.getMessage("excel.colResultName"))
        }
        else {
            headerRow.getCell(colIndexResult).setCellValue(CommonUtils.getMessage("excel.colResultName"))
        }
        val headerStyle = headerRow.getCell(0).cellStyle
        headerRow.getCell(colIndexResult).cellStyle.cloneStyleFrom(headerStyle)
        headerRow.getCell(colIndexResult).cellStyle.fillForegroundColor = IndexedColors.RED.index
        headerRow.getCell(colIndexResult).cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        sheet.setColumnWidth(colIndexResult, 15000)

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val name = ExcelHelper.getCellValue(row, 0)
            val messageResults = validateImportProduct(row, headerRow, masterData)
            total++
            if (messageResults.isEmpty()) {
                val productExist = productExists.find { x -> x.name == name }
                try {
                    if (productExist == null) {
                        val product = Product(
                            name = ExcelHelper.getCellValue(row, 0),
                            exportType = ExcelHelper.getCellValue(row, 1),
                            size = ExcelHelper.getCellValue(row, 2),
                            frame_1 = ExcelHelper.getCellValue(row, 3),
                            frame_2 = ExcelHelper.getCellValue(row, 4),
                            mold = ExcelHelper.getCellValue(row, 5),
                            productLine = ExcelHelper.getCellValue(row, 6),
                            srNosr = ExcelHelper.getCellValue(row, 7),
                            pcsSh = ExcelHelper.getCellValue(row, 8).toBigDecimalOrNull()?.toInt(),
                            shBlock = ExcelHelper.getCellValue(row, 9).toBigDecimalOrNull()?.toInt(),
                            layerCount = ExcelHelper.getCellValue(row, 10).toBigDecimalOrNull()?.toInt(),
                            ringJig = ExcelHelper.getCellValue(row, 11),
                            process = ExcelHelper.getCellValue(row, 12).toBigDecimalOrNull()?.toInt(),
                            snapMold = ExcelHelper.getCellValue(row, 13),
                            tapeCommon = ExcelHelper.getCellValue(row, 14),
                            tapeType = ExcelHelper.getCellValue(row, 15),
                        )

                        val layers = mutableListOf<LayerImportProductModel>()
                        for (i in 16 until colIndexResult) {
                            if (ExcelHelper.getCellValue(row, i).isEmpty()) continue
                            val layer = LayerImportProductModel((i - 15).toString(), ExcelHelper.getCellValue(row, i).toBigDecimalOrNull()?.toInt())
                            layers.add(layer)
                        }

                        product.productLayerDetail = JsonConvert.serialize(layers)

                        productRep.add(product)
                    } else {
                        productExist.exportType = ExcelHelper.getCellValue(row, 1)
                        productExist.size = ExcelHelper.getCellValue(row, 2)
                        productExist.frame_1 = ExcelHelper.getCellValue(row, 3)
                        productExist.frame_2 = ExcelHelper.getCellValue(row, 4)
                        productExist.mold = ExcelHelper.getCellValue(row, 5)
                        productExist.productLine = ExcelHelper.getCellValue(row, 6)
                        productExist.srNosr = ExcelHelper.getCellValue(row, 7)
                        productExist.pcsSh = ExcelHelper.getCellValue(row, 8).toBigDecimalOrNull()?.toInt()
                        productExist.shBlock = ExcelHelper.getCellValue(row, 9).toBigDecimalOrNull()?.toInt()
                        productExist.layerCount = ExcelHelper.getCellValue(row, 10).toBigDecimalOrNull()?.toInt()
                        productExist.ringJig = ExcelHelper.getCellValue(row, 11)
                        productExist.process = ExcelHelper.getCellValue(row, 12).toBigDecimalOrNull()?.toInt()
                        productExist.snapMold = ExcelHelper.getCellValue(row, 13)
                        productExist.tapeCommon = ExcelHelper.getCellValue(row, 14)
                        productExist.tapeType = ExcelHelper.getCellValue(row, 15)

                        val layers = mutableListOf<LayerImportProductModel>()
                        for (i in 16 until colIndexResult) {
                            if (ExcelHelper.getCellValue(row, i).isEmpty()) continue
                            val layer = LayerImportProductModel((i - 15).toString(), ExcelHelper.getCellValue(row, i).toBigDecimalOrNull()?.toInt())
                            layers.add(layer)
                        }

                        productExist.productLayerDetail = JsonConvert.serialize(layers)

                        productRep.update(productExist)
                    }
                    messageResults.add(CommonUtils.getMessage("validate.excel.importSuccess"))
                    count++
                } catch (e: Exception) {
                    messageResults.add(CommonUtils.getMessage("validate.excel.updateDataError"))
                }
            }
            val result = messageResults.joinToString(separator = "; ")

            if (row.getCell(colIndexResult) == null) {
                row.createCell(colIndexResult)
            }
            row.getCell(colIndexResult).setCellValue(result)
            row.getCell(colIndexResult).cellStyle = style
        }



        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.resultImportProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = Constants.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(
            response,
            if(count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total))
        )
    }

    private fun mappingProductResponse(products: List<Product>) : PagingProductResponse {
        val productNames = products.mapNotNull { x -> x.name }
        val completionRates = completionRateProductRep.getByProduct(productNames)
        val productProcesses = processProcedureStructureRep.getByProductName(productNames)
        val processGroups = productProcesses.groupBy { x -> Pair(x.productCode, x.processCode) }

        val response = PagingProductResponse()
        response.data = products.map { x -> ProductModel(
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
            completionRate = completionRates.find { m -> m.productName == x.name }?.rate?.toDouble(),
            lstProcess = processGroups.filter { m -> m.key.first == x.name }.mapNotNull { m -> KeyValueResponse(m.key.second, m.value.size.toString()) }
        ) }

        response.columns = productProcesses.map { x -> KeyValueResponse(x.processCode,x.processCode) }.distinct()

        return response
    }

    private fun validateImportProduct(row: Row, headerRow: Row, masterData: MasterDataSelectionResponse) : MutableList<String> {
        val messageResults = mutableListOf<String>()
        if (ExcelHelper.getCellValue(row, 0).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 0))))
        }
        else {
            if (ExcelHelper.getCellValue(row, 0).length != 12)
                messageResults.add(CommonUtils.getMessage("validate.excel.length", arrayOf(ExcelHelper.getCellValue(headerRow, 0), "12")))
        }
        if (ExcelHelper.getCellValue(row, 1).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 1))))
        }
        else {
            if (!masterData.exportTypeSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 1) })
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 1))))
        }
        if (ExcelHelper.getCellValue(row, 2).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 2))))
        }
        if (ExcelHelper.getCellValue(row, 3).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
        }
        else {
            if (!masterData.frame1Selections.any { x -> x.label == ExcelHelper.getCellValue(row, 3) })
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
        }
        if (ExcelHelper.getCellValue(row, 4).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 4))))
        }
        else {
            if (!masterData.frame2Selections.any { x -> x.label == ExcelHelper.getCellValue(row, 4) })
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 4))))
        }
        if (ExcelHelper.getCellValue(row, 5).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 5))))
        }
        else {
            if (!masterData.moldSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 5) })
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 5))))
        }
        if (ExcelHelper.getCellValue(row, 7).isNotEmpty() && !masterData.srNosrSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 7) }) {
            messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 7))))
        }
        if (ExcelHelper.getCellValue(row, 8).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 8))))
        }
        if (ExcelHelper.getCellValue(row, 9).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 9))))
        }
        if (ExcelHelper.getCellValue(row, 10).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 10))))
        }
        if (ExcelHelper.getCellValue(row, 11).isNotEmpty() && !masterData.ringJigSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 11) }) {
            messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 11))))
        }
        if (ExcelHelper.getCellValue(row, 12).isNotEmpty() && ExcelHelper.getCellValue(row, 12).toBigDecimalOrNull() == null) {
            messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, 12))))
        }
        if (ExcelHelper.getCellValue(row, 14).isNotEmpty() && !masterData.tapeCommonSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 14) }) {
            messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 14))))
        }
        if (ExcelHelper.getCellValue(row, 15).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 15))))
        }
        else {
            if (!masterData.tapeTypeSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 15) }) {
                messageResults.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 15))))
            }
        }
        val frame1 = ExcelHelper.getCellValue(row, 3)
        val mold = ExcelHelper.getCellValue(row, 5)
        if (frame1.isNotEmpty() && mold.isNotEmpty()) {
            when (frame1) {
                Constants.KHUNG1_ML -> {
                    if(mold != Constants.KHUONDUC_ML)
                        messageResults.add(CommonUtils.getMessage("validate.excel.fieldMatching", arrayOf(ExcelHelper.getCellValue(headerRow, 5), ExcelHelper.getCellValue(headerRow, 3))))
                }
                Constants.KHUNG1_MU -> {
                    if(mold != Constants.KHUONDUC_KVC && mold != Constants.KHUONDUC_SKE)
                        messageResults.add(CommonUtils.getMessage("validate.excel.fieldMatching", arrayOf(ExcelHelper.getCellValue(headerRow, 5), ExcelHelper.getCellValue(headerRow, 3))))
                }
                Constants.KHUNG1_SWR -> {
                    if(mold != Constants.KHUONDUC_SWR && mold != Constants.KHUONDUC_SUR)
                        messageResults.add(CommonUtils.getMessage("validate.excel.fieldMatching", arrayOf(ExcelHelper.getCellValue(headerRow, 5), ExcelHelper.getCellValue(headerRow, 3))))
                }
            }
        }
        return messageResults
    }


}