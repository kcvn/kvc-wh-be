package com.kcvn.spm.app.inventoryproduct.service

import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.StringHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.InventoryIns_30day
import com.kcvn.spm.repository.InventoryIns30DayRepository
import com.kcvn.spm.repository.ProductRepository
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class InventoryIns30DayService(
    private val inventoryIns30DayRep: InventoryIns30DayRepository,
    private val productRep: ProductRepository,
) {

    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportInventoryIns30DayTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importInventoryIns30DayTemplate"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun checkInventory(date: OffsetDateTime): BaseResponse<Boolean> {
        val isNoInventory = inventoryIns30DayRep.isNoInventoryByDate(date)
        val formattedDate = DateTimeHelper.convertOffSetDateTimeUtc7ToString(date)

        return BaseResponse(
            isNoInventory,
            if (isNoInventory) null else CommonUtils.getMessage("check.inventoryDateProduct", arrayOf(formattedDate.toString()))
        )
    }

    fun importInventory(date: OffsetDateTime, file: MultipartFile): BaseResponse<FileContentModel> {
        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportInventoryIns30DayTemplate.xlsx"
        val workbook = WorkbookFactory.create(file.inputStream)

        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1

            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))

            val headerRow = sheet.getRow(0)

            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 28))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

            var count = 0
            var total = 0
            val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)

            val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 9) }
            val products = productRep.getByName(productNames)

            inventoryIns30DayRep.deleteByDate(date)

            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val style = row.getCell(0).cellStyle
                val productName = ExcelHelper.getCellValue(row, 9)
                var code = ExcelHelper.getCellValue(row, 14)
                if (code.toBigDecimalOrNull() != null) {
                    code = code.toBigDecimal().toBigInteger().toString()
                }
                val messageResults = validateImportInventory(row, headerRow)

                val product = products.find { x -> x.name == productName }
                if (product == null) messageResults.add(CommonUtils.getMessage("product.not.exist"))

                total++
                if (messageResults.isEmpty()) {
                    try {
                        val inventory = InventoryIns_30day(
                            inventoryDate = date,
                            productName = productName,
                            code = code,
                            processCode = ExcelHelper.getCellValue(row, 2).toBigDecimalOrNull()?.toInt().toString(),
                            layerCode = StringHelper.intToStringD2(ExcelHelper.getCellValue(row, 15).toBigDecimalOrNull()?.toInt()),
                            productQuantity = ExcelHelper.getCellValue(row, 20).toBigDecimalOrNull()?.toInt() ?: 0,
                        )

                        inventoryIns30DayRep.add(inventory)

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
                row.getCell(colIndexResult).cellStyle = ExcelHelper.getCellStyleResultCol(workbook, style)
            }

            if (count == total) {
                return BaseResponse(null, CommonUtils.getMessage("import.success", arrayOf(count, total)))
            }

            val resultRows = sheet.filter {
                x -> ExcelHelper.getCellValue(x, colIndexResult) != CommonUtils.getMessage("validate.excel.importSuccess")
                && x.rowNum >= rowIndex
            }

            val response = exportErrorFile(resultRows, headerRow, workbook, sheet)

            return BaseResponse(
                response,
                if (count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total))
            )
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }

    private fun exportErrorFile(
        dataRows: List<Row>,
        titleRow: Row,
        workbook: Workbook,
        importSheet: Sheet
    ): FileContentModel {
        val sheet = workbook.createSheet()
        val headerRow: Row = sheet.getRow(0) ?: sheet.createRow(0)
        headerRow.height = titleRow.height

        val style = ExcelHelper.getCellStyleCommon(workbook)
        style.alignment = HorizontalAlignment.CENTER

        for (i in 0 until titleRow.lastCellNum) {
            val headerStyle = titleRow.getCell(i).cellStyle
            val headerCellValue = ExcelHelper.getCellValue(titleRow, i)
            ExcelHelper.setCellValue(headerRow, i, headerStyle, headerCellValue)
            sheet.setColumnWidth(i, importSheet.getColumnWidth(i))
        }

        val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)
        val rowHeight = importSheet.getRow(1).height
        var rowNumber = 1

        for (iRow in dataRows) {
            val dataRow: Row = sheet.createRow(rowNumber)
            dataRow.height = rowHeight
            for (iCol in 0 until iRow.lastCellNum.toInt()) {
                val cellValue = ExcelHelper.getCellValue(iRow, iCol)
                val cellStyle = iRow.getCell(iCol)?.cellStyle ?: style
                ExcelHelper.setCellValue(dataRow, iCol, cellStyle, cellValue)
            }
            rowNumber++
        }

        workbook.removeSheetAt(0)
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.resultImportInventoryIns30Day", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return response
    }

    private fun validateImportInventory(row: Row, headerRow: Row): MutableList<String> {
        val messageResults = mutableListOf<String>()
        if (ExcelHelper.getCellValue(row, 9).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 9))))
        } else {
            if (ExcelHelper.getCellValue(row, 9).length != 12)
                messageResults.add(CommonUtils.getMessage("validate.excel.length", arrayOf(ExcelHelper.getCellValue(headerRow, 9), "12")))
        }

        if (ExcelHelper.getCellValue(row, 2).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 2))))
        }

        if (ExcelHelper.getCellValue(row, 14).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 14))))
        }

        if (ExcelHelper.getCellValue(row, 15).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 15))))
        }

        if (ExcelHelper.getCellValue(row, 20).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 20))))
        } else {
            if (ExcelHelper.getCellValue(row, 20).toBigDecimalOrNull() == null)
                messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, 20))))
        }

        return messageResults
    }
}

