package com.kcvn.spm.app.inventoryproduct.service

import com.kcvn.spm.app.inventoryproduct.payload.response.CheckInventoryDateResponse
import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.excelhelper.ExcelHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.InventoryProduct
import com.kcvn.spm.repository.InventoryProductRepository
import com.kcvn.spm.repository.ProcessProcedureStructureRepository
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.text.SimpleDateFormat
import java.time.*
import java.time.format.DateTimeFormatter


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
        val total = sheet.lastRowNum - rowIndex


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

        val dateFormat = SimpleDateFormat("dd/MM/yyyy");

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
            if (row.getCell(0) != null && !isDateTime(row.getCell(0).toString(), dateFormat)) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.invalidDatetime",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 0), 12)
                    )
                )
            }
            if (row.getCell(1) != null && row.getCell(1).toString().length > 8) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 1), 6)
                    )
                )
            }
            if (row.getCell((2)) != null && row.getCell(2).toString().length > 50) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 2), 50)
                    )
                )
            }
            if (row.getCell(3) != null && row.getCell(3).toString().length > 4) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 3), 2)
                    )
                )
            }
            if (row.getCell(4) != null && row.getCell(4).toString().length > 100) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 4), 100)
                    )
                )
            }
            if (row.getCell(5) != null && row.getCell(5).toString().length > 12) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 5), 12)
                    )
                )
            }
            if (row.getCell(6) != null && row.getCell(6).toString().length > 50) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.maxLength",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 6), 50)
                    )
                )
            }
            if (row.getCell(7) != null && row.getCell(7).cellType != CellType.NUMERIC) {
                check = false
                messageResults.add(
                    CommonUtils.getMessage(
                        "validate.excel.isNumber",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 7), 100)
                    )
                )
            }
            if (row.getCell(8) != null && row.getCell(8).cellType != CellType.NUMERIC) {
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
                    val filterCheckProcessProcedure = processProcedureRep.getByFilterProcessStructure(filter)
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
                        messageResults.add("OK")
                        count++
                    }
                }
            }
            catch (e: Exception){
                messageResults.add(CommonUtils.getMessage("validate.excel.inventoryProduct.data.update.err"))
            }
            val result = messageResults.joinToString(separator = "; ")

            if (!checkColResult) {
                row.createCell(row.lastCellNum + 0).setCellValue(result)
                row.getCell(row.lastCellNum - 1).cellStyle = style
            }
            else{
                row.getCell(row.lastCellNum - 2).setCellValue(result)
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
}

fun isDateTime(input: String, dateFormat: SimpleDateFormat): Boolean {
    dateFormat.isLenient = false
    return try {
        dateFormat.parse(input)
        true
    } catch (e: Exception) {
        false
    }
}