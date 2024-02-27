package com.kcvn.spm.app.inventoryproduct.service

import com.kcvn.spm.app.inventoryproduct.payload.request.InventoryProductRequest
import com.kcvn.spm.app.inventoryproduct.payload.response.CheckInventoryDateResponse
import com.kcvn.spm.app.inventoryproduct.payload.response.InventoryProductResponse
import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.InventoryProduct
import com.kcvn.spm.repository.InventoryProductRepository
import com.kcvn.spm.repository.ProcessProcedureStructureRepository
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.time.*
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException


@Service
@Transactional
class InventoryProductService(
    private val inventoryProductRepository: InventoryProductRepository,
    private val processProcedureRep : ProcessProcedureStructureRepository

) {
    fun checkInventoryDate(date: OffsetDateTime) : CheckInventoryDateResponse?{
        val data = CheckInventoryDateResponse()
        val query = inventoryProductRepository.findDateInventoryProduct(date)
        if(query != null) {
            data.hasInventoryDate = true
            return data
        }
        return data
    }

    fun downloadTemplate() : BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportInventoryProduct.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "ImportInventoryProduct.xlsx",
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )

        return BaseResponse(response)
    }

    fun importExelInventoryProduct(file: MultipartFile) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex }) throw BusinessException(CommonUtils.getMessage("import.file.empty"))
        var count = 0
        val total = sheet.lastRowNum


        val headerCell = sheet.first().lastCellNum + 0
        val headerRow = sheet.getRow(0)

        val checkColResult = ExcelHelper.getCellValue(headerRow, headerCell - 1) == "Kết quả"
        if (!checkColResult) {
            headerRow.createCell(headerCell).setCellValue("Kết quả")
            val headerStyle = headerRow.getCell(0).cellStyle
            headerRow.getCell(headerCell).cellStyle.cloneStyleFrom(headerStyle)
            headerRow.getCell(headerCell).cellStyle.fillForegroundColor = IndexedColors.RED.index
            headerRow.getCell(headerCell).cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            sheet.setColumnWidth(headerCell, 15000)
        }

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportInventoryProduct.xlsx"

        if (ExcelHelper.fileIsEmpty(
                sheet,
                rowIndex
            )
        ) throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 5))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

        val colEmpty = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == "" }
        val colResult = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == CommonUtils.getMessage("excel.colResultName") }
        val colIndexResult = colResult?.columnIndex ?: (colEmpty?.columnIndex ?: (sheet.first().lastCellNum + 0))

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle
            val messageResults = mutableListOf<String>()
            var check = true
            if (ExcelHelper.getCellValue(row, 0).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 0))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 1).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 0))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 2).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 1))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 3).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 2))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 4).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 3))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 5).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 4))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 6).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 5))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 7).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 6))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 8).isEmpty()) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 7))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 8).isNotEmpty() && !isDateValid(ExcelHelper.getCellValue(row, 0))) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.invalidDatetime",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 0))
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 1).isNotEmpty() && row.getCell(1).toString().length > 8) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 1), 6)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 2).isNotEmpty() && row.getCell(2).toString().length > 50) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 2), 50)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 3).isNotEmpty() && row.getCell(3).toString().length > 4) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 3), 2)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 3).isNotEmpty() && row.getCell(4).toString().length > 100) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 4), 100)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 5).isNotEmpty() && row.getCell(5).toString().length > 12) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 5), 12)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 6).isNotEmpty() && row.getCell(6).toString().length > 50) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 6), 50)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 7).isNotEmpty() && row.getCell(7).cellType != CellType.NUMERIC) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.isNumber",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 7), 100)
                    )
                )
            }
            if (ExcelHelper.getCellValue(row, 8).isNotEmpty() && row.getCell(8).cellType != CellType.NUMERIC) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.isNumber",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 8), 100)
                    )
                )
            }

            try {
                if(check) {
                    val cellProcessCode = row.getCell(1)
                    var processCode = ""
                    if(cellProcessCode.cellType == CellType.NUMERIC && cellProcessCode.numericCellValue % 1 == 0.0)
                        processCode = cellProcessCode.numericCellValue.toInt().toString()
                    else {
                        processCode = ExcelHelper.getCellValue(row, 1)
                    }

                    val cellLayerCode = row.getCell(3)
                    var layerCode = ""
                    if(cellLayerCode.cellType == CellType.NUMERIC && cellLayerCode.numericCellValue % 1 == 0.0)
                        layerCode = cellLayerCode.numericCellValue.toInt().toString()
                    else {
                        layerCode = ExcelHelper.getCellValue(row, 3)
                    }
                    val filter = ImportProcessRequest(
                        productName = ExcelHelper.getCellValue(row, 5),
                        processCode = processCode,
                        layerCode = layerCode
                    )
                    val filterCheckProcessProcedure = processProcedureRep.getByFilterProcessStructureByInventoryProduct(filter)
                    if(filterCheckProcessProcedure == null)
                    {
                        messageResults.add(CommonUtils.getMessage("validate.excel.inventoryProduct.dataNull"))
                    }else {
                        val inventoryDate = ExcelHelper.getCellValue(row, 0)
                        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy") // Định dạng của chuỗi
                        val localDate = LocalDate.parse(inventoryDate, formatter) // Chuyển đổi chuỗi thành LocalDate
                        val offsetDateTime = OffsetDateTime.of(localDate, LocalTime.MIN, ZoneOffset.UTC)
                        val requestImport = InventoryProduct(
                            processProcedureStructureId = filterCheckProcessProcedure.id,
                            inventoryDate = offsetDateTime,
                            code = ExcelHelper.getCellValue(row, 2),
                            tapeLotNo = ExcelHelper.getCellValue(row, 4),
                            orderCode = ExcelHelper.getCellValue(row, 6),
                            productQuantity = ExcelHelper.getCellValue(row, 7).toDouble().toInt(),
                            sheetQuantity = ExcelHelper.getCellValue(row, 8).toDouble().toInt()
                        )

                        val checkInventoryProduct = inventoryProductRepository.findInventoryProduct(filterCheckProcessProcedure.id)

                        if(checkInventoryProduct == null) {
                            requestImport.createdDate = LocalDateTime.now().atOffset(ZoneOffset.UTC)
                            requestImport.createdBy = CommonUtils.loggedInUser() ?: "SYSTEM"
                            inventoryProductRepository.insertInventoryProduct(requestImport)
                        }else {
                            requestImport.updatedBy = CommonUtils.loggedInUser() ?: "SYSTEM"
                            requestImport.updatedDate = LocalDateTime.now().atOffset(ZoneOffset.UTC)
                            inventoryProductRepository.updateInventoryProduct(requestImport)
                        }
                        messageResults.add(CommonUtils.getMessage("validate.excel.importSuccess"))
                        count++
                    }
                }
            }
            catch (e: Exception){
                messageResults.add(CommonUtils.getMessage("validate.excel.inventoryProduct.data.update.err"))
            }
            val result = messageResults.joinToString(separator = "; ")

            if (row.getCell(colIndexResult) == null) {
                row.createCell(colIndexResult)
            }
            row.getCell(colIndexResult ).setCellValue(result)
            row.getCell(colIndexResult ).cellStyle = style
        }

        val resultRows = sheet.filter { x ->  ExcelHelper.getCellValue(x, colIndexResult) == CommonUtils.getMessage("validate.excel.importSuccess") }
        for (row in resultRows) {
            val rowNum = row.rowNum
            sheet.removeRow(row)
            if (rowNum >= 0 && rowNum < sheet.lastRowNum) {
                sheet.shiftRows(rowNum + 1, sheet.lastRowNum, -1)
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.excel.result.import",arrayOf(LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            content = excelBytes
        )
        workbook.close()

        return BaseResponse(
            response,
            if(count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total))
        )
    }

    fun getListInventoryProduct(request: InventoryProductRequest? , pageable: Pageable) : BasePagingResponse<InventoryProductResponse?> {
        val result = inventoryProductRepository.findByKeywordPaginated(request,pageable)
        val response = BasePagingResponse<InventoryProductResponse?>()
        response.data = result.first.map { inventoryProduct ->
            InventoryProductResponse(
                inventoryDate = inventoryProduct?.inventoryDate,
                productQuantity = inventoryProduct?.productQuantity,
                sheetQuantity = inventoryProduct?.sheetQuantity,
                orderCode = inventoryProduct?.orderCode,
                tapeLotNo = inventoryProduct?.tapeLotNo,
                processCode = inventoryProduct?.processCode,
                productName = inventoryProduct?.productName,
                processName = inventoryProduct?.processName,
                code = inventoryProduct?.code,
                pcsSh = inventoryProduct?.pcsSh,
                layerCode = inventoryProduct?.layerCode,
            )
        }
        response.totalRecords = result.second ?: 0
        return  response
    }

    fun exportExcel(request: InventoryProductRequest? , pageable: Pageable) : BaseResponse<FileContentModel>{
        val inventoryProduct = inventoryProductRepository.findByKeywordPaginated(request,pageable)

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportInventoryProduct.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)
        val headerRow = sheet.getRow(0)
        if (inventoryProduct.first.isNotEmpty()) {
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

            val colEmpty = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == "" }
            val colResult = headerRow.firstOrNull { x -> ExcelHelper.getCellValue(headerRow, x.columnIndex) == CommonUtils.getMessage("excel.colResultName") }
            val colIndexResult = colResult?.columnIndex ?: (colEmpty?.columnIndex ?: (sheet.first().lastCellNum + 0))


            var rowNumber = 1
            for (item in inventoryProduct.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                val localDate = item?.inventoryDate?.toLocalDate() // Chuyển đổi OffsetDateTime thành LocalDate
                val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy") // Định dạng của chuỗi
                val formattedDate = localDate?.format(formatter) // Định dạng lại LocalDate thành chuỗi
                dataRow.createCell(0).setCellValue(formattedDate)
                dataRow.getCell(0).cellStyle = style

                dataRow.createCell(1).setCellValue(item?.productName)
                dataRow.getCell(1).cellStyle = style

                dataRow.createCell(2).setCellValue(item?.processName)
                dataRow.getCell(2).cellStyle = style

                dataRow.createCell(3).setCellValue(item?.processCode)
                dataRow.getCell(3).cellStyle = style

                dataRow.createCell(4).setCellValue(item?.layerCode)
                dataRow.getCell(4).cellStyle = style

                dataRow.createCell(5).setCellValue(item?.pcsSh)
                dataRow.getCell(5).cellStyle = style

                dataRow.createCell(6).setCellValue(item?.productQuantity.toString())
                dataRow.getCell(6).cellStyle = style

                dataRow.createCell(7).setCellValue(item?.sheetQuantity.toString())
                dataRow.getCell(7).cellStyle = style

                dataRow.createCell(8).setCellValue(item?.orderCode)
                dataRow.getCell(8).cellStyle = style

                dataRow.createCell(9).setCellValue(item?.tapeLotNo)
                dataRow.getCell(9).cellStyle = style

                dataRow.createCell(10).setCellValue(item?.code)
                dataRow.getCell(10).cellStyle = style
            }
            val resultRows = sheet.filter { x ->  ExcelHelper.getCellValue(x, colIndexResult) == CommonUtils.getMessage("validate.excel.importSuccess") }
            for (row in resultRows) {
                val rowNum = row.rowNum
                sheet.removeRow(row)
                if (rowNum >= 0 && rowNum < sheet.lastRowNum) {
                    sheet.shiftRows(rowNum + 1, sheet.lastRowNum, -1)
                }
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.excel.inventoryProduct",arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = Constants.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }
}

fun isDateValid(dateStr: String): Boolean {
    val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")
    return try {
        formatter.parse(dateStr)
        true // Nếu không có lỗi, định dạng là hợp lệ
    } catch (e: DateTimeParseException) {
        var test = e.message;
        false // Nếu có lỗi, định dạng không hợp lệ
    }
}