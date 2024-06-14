package com.kcvn.spm.app.inventoryproduct.service

import com.kcvn.spm.app.inventoryproduct.payload.model.ImportInventorySemiProductErrorModel
import com.kcvn.spm.app.inventoryproduct.payload.request.InventorySemiProductSearchRequest
import com.kcvn.spm.app.inventoryproduct.payload.response.InventorySemiProductResponse
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.NumberHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.CellStyleModel
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.InventorySemiProduct
import com.kcvn.spm.repository.CompletionRateProductRepository
import com.kcvn.spm.repository.InventorySemiProductRepository
import com.kcvn.spm.repository.ProductRepository
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
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
class InventorySemiProductService(
    private val inventorySemiProductRep: InventorySemiProductRepository,
    private val productRep: ProductRepository,
    private val completionRateProductRep: CompletionRateProductRepository
) {

    fun getList(request: InventorySemiProductSearchRequest, pageable: Pageable): BasePagingResponse<InventorySemiProductResponse> {
        val inventories = inventorySemiProductRep.getList(request, pageable)
        val data = mappingInventorySemiProduct(inventories.first)
        return BasePagingResponse(
            data,
            inventories.second
        )
    }

    fun exportExcel(request: InventorySemiProductSearchRequest, pageable: Pageable): BaseResponse<FileContentModel> {
        val inventories = inventorySemiProductRep.getList(request, pageable, true)
        if (inventories.first.isEmpty()) throw BusinessException(CommonUtils.getMessage("excel.export.noData"))
        val data = mappingInventorySemiProduct(inventories.first)

        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ExportInventorySemiProductTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        val style = ExcelHelper.getCellStyleCommon(workbook)
        style.alignment = HorizontalAlignment.CENTER

        val numberStyle = workbook.createCellStyle()
        numberStyle.cloneStyleFrom(style)
        numberStyle.alignment = HorizontalAlignment.RIGHT

        var rowNumber = 1

        for (item in data) {
            val dataRow: Row = ExcelHelper.createRow(sheet, rowNumber)
            val invDate = DateTimeHelper.toString(
                DateTimeHelper.toTimeZone7(item.inventoryDate)!!,
                DateTimeFormat.dd_MM_yyyy
            )
            ExcelHelper.setCellValue(dataRow, 0, style, invDate)
            ExcelHelper.setCellValue(dataRow, 1, style, item.productName)
            ExcelHelper.setCellValue(dataRow, 2, style, item.tapeLotNo)
            ExcelHelper.setCellValue(dataRow, 3, style, if (item.completionRate != null) "${item.completionRate}%" else "")
            ExcelHelper.setCellValue(dataRow, 4, style, NumberHelper.formatNumber(item.blockSh))
            ExcelHelper.setCellValue(dataRow, 5, numberStyle, NumberHelper.formatNumber(item.setQuantity))
            ExcelHelper.setCellValue(dataRow, 6, numberStyle, NumberHelper.formatNumber(item.blockQuantity))
            ExcelHelper.setCellValue(dataRow, 7, numberStyle, NumberHelper.formatNumber(item.sumBlockQuantity))
            ExcelHelper.setCellValue(dataRow, 8, numberStyle, NumberHelper.formatNumber(item.ngBlockQuantity))
            ExcelHelper.setCellValue(dataRow, 9, numberStyle, NumberHelper.formatNumber(item.successBlockQuantity))
            rowNumber++
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportListInventorySemiProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    private fun mappingInventorySemiProduct(inventories: List<InventorySemiProduct>): List<InventorySemiProductResponse> {
        if (inventories.isEmpty()) return listOf()

        val productNames = inventories.mapNotNull { it.productName }.distinct()
        val products = productRep.getByName(productNames)
        val completionRates = completionRateProductRep.getEffectiveByProduct(productNames)

        val response = inventories.map { item ->
            val product = products.find { it.name == item.productName }
            val completionRate = completionRates?.find { it.productName == item.productName }
            InventorySemiProductResponse(
                inventoryDate = item.inventoryDate,
                productName = item.productName,
                tapeLotNo = item.tapeLotNo,
                completionRate = completionRate?.rate,
                blockSh = product?.shBlock,
                setQuantity = item.setQuantity,
                blockQuantity = item.blockQuantity,
                sumBlockQuantity = item.sumBlockQuantity,
                ngBlockQuantity = item.ngBlockQuantity,
                successBlockQuantity = item.successBlockQuantity
            )
        }

        return response
    }

    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportInventorySemiProductTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importInventorySemiProductTemplate"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun checkInventory(date: OffsetDateTime): BaseResponse<Boolean> {
        val isNoInventory = inventorySemiProductRep.isNoInventoryByDate(date)
        val formattedDate = DateTimeHelper.convertOffSetDateTimeUtc7ToString(date)

        return BaseResponse(
            isNoInventory,
            if (isNoInventory) null else CommonUtils.getMessage("check.inventoryDateProduct", arrayOf(formattedDate.toString()))
        )
    }

    fun importInventory(date: OffsetDateTime, file: MultipartFile): BaseResponse<FileContentModel> {
        inventorySemiProductRep.deleteByDate(date)

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportInventorySemiProductTemplate.xlsx"
        val workbook = WorkbookFactory.create(file.inputStream)

        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1

            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))

            val headerRow = sheet.getRow(0)

            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 7))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

            var count = 0
            var total = 0
            val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)

            val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
            val products = productRep.getByName(productNames)

            val dataInserts = mutableListOf<Pair<String, String>>()

            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val style = row.getCell(0).cellStyle
                val productName = ExcelHelper.getCellValue(row, 0)
                val tapeLotNo = ExcelHelper.getCellValue(row, 1)
                val messageResults = validateImportInventory(row, headerRow)
                if (dataInserts.any { it.first == productName && it.second == tapeLotNo })
                    messageResults.add(CommonUtils.getMessage("validate.excel.duplicate"))

                val product = products.find { x -> x.name == productName }
                if (product == null) messageResults.add(CommonUtils.getMessage("product.not.exist"))

                total++
                if (messageResults.isEmpty()) {
                    try {
                        val inventory = InventorySemiProduct(
                            inventoryDate = date,
                            productName = productName,
                            tapeLotNo = tapeLotNo,
                            setQuantity = ExcelHelper.getCellValue(row, 2).toIntOrNull() ?: 0,
                            blockQuantity = ExcelHelper.getCellValue(row, 3).toIntOrNull() ?: 0,
                            sumBlockQuantity = ExcelHelper.getCellValue(row, 4).toIntOrNull() ?: 0,
                            ngBlockQuantity = ExcelHelper.getCellValue(row, 5).toIntOrNull() ?: 0,
                            successBlockQuantity = ExcelHelper.getCellValue(row, 6).toIntOrNull() ?: 0
                        )

                        inventorySemiProductRep.add(inventory)

                        messageResults.add(CommonUtils.getMessage("validate.excel.importSuccess"))
                        count++
                        dataInserts.add(Pair(productName, tapeLotNo))
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
            }.map {
                ImportInventorySemiProductErrorModel(
                    productName = ExcelHelper.getCellValue(it, 0),
                    tapeLotNo = ExcelHelper.getCellValue(it, 1),
                    setQuantity = ExcelHelper.getCellValue(it, 2).toIntOrNull(),
                    blockQuantity = ExcelHelper.getCellValue(it, 3).toIntOrNull(),
                    sumBlockQuantity = ExcelHelper.getCellValue(it, 4).toIntOrNull(),
                    ngBlockQuantity = ExcelHelper.getCellValue(it, 5).toIntOrNull(),
                    successBlockQuantity = ExcelHelper.getCellValue(it, 6).toIntOrNull(),
                    messageError = ExcelHelper.getCellValue(it, colIndexResult),
                    cellStyles = it.map { m -> CellStyleModel(m.columnIndex, m.cellStyle) }
                )
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
        inventories: List<ImportInventorySemiProductErrorModel>,
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

        for (item in inventories) {
            val dataRow: Row = sheet.createRow(rowNumber)
            dataRow.height = rowHeight
            ExcelHelper.setCellValue(dataRow, 0, item.cellStyles.find { x -> x.index == 0 }?.cellStyle ?: style, item.productName)
            ExcelHelper.setCellValue(dataRow, 1, item.cellStyles.find { x -> x.index == 1 }?.cellStyle ?: style, item.tapeLotNo)
            ExcelHelper.setCellValue(dataRow, 2, item.cellStyles.find { x -> x.index == 2 }?.cellStyle ?: style, item.setQuantity?.toString())
            ExcelHelper.setCellValue(dataRow, 3, item.cellStyles.find { x -> x.index == 3 }?.cellStyle ?: style, item.blockQuantity?.toString())
            ExcelHelper.setCellValue(dataRow, 4, item.cellStyles.find { x -> x.index == 4 }?.cellStyle ?: style, item.sumBlockQuantity?.toString())
            ExcelHelper.setCellValue(dataRow, 5, item.cellStyles.find { x -> x.index == 5 }?.cellStyle ?: style, item.ngBlockQuantity?.toString())
            ExcelHelper.setCellValue(dataRow, 6, item.cellStyles.find { x -> x.index == 6 }?.cellStyle ?: style, item.successBlockQuantity?.toString())
            ExcelHelper.setCellValue(dataRow, colIndexResult, item.cellStyles.find { x -> x.index == colIndexResult }?.cellStyle ?: style, item.messageError)
            rowNumber++
        }

        workbook.removeSheetAt(0)
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.resultImportInventorySemiProduct", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return response
    }

    private fun validateImportInventory(row: Row, headerRow: Row): MutableList<String> {
        val messageResults = mutableListOf<String>()
        if (ExcelHelper.getCellValue(row, 0).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 0))))
        } else {
            if (ExcelHelper.getCellValue(row, 0).length != 12)
                messageResults.add(CommonUtils.getMessage("validate.excel.length", arrayOf(ExcelHelper.getCellValue(headerRow, 0), "12")))
        }

        if (ExcelHelper.getCellValue(row, 1).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 1))))
        }

        if (ExcelHelper.getCellValue(row, 2).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 2))))
        } else {
            if (ExcelHelper.getCellValue(row, 2).toBigDecimalOrNull() == null)
                messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, 2))))
        }

        if (ExcelHelper.getCellValue(row, 3).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
        } else {
            if (ExcelHelper.getCellValue(row, 3).toBigDecimalOrNull() == null)
                messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
        }

        if (ExcelHelper.getCellValue(row, 4).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 4))))
        } else {
            if (ExcelHelper.getCellValue(row, 4).toBigDecimalOrNull() == null)
                messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, 4))))
        }

        if (ExcelHelper.getCellValue(row, 5).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 5))))
        } else {
            if (ExcelHelper.getCellValue(row, 5).toBigDecimalOrNull() == null)
                messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, 5))))
        }

        if (ExcelHelper.getCellValue(row, 6).isEmpty()) {
            messageResults.add(CommonUtils.getMessage("validate.excel.empty", arrayOf(ExcelHelper.getCellValue(headerRow, 6))))
        } else {
            if (ExcelHelper.getCellValue(row, 6).toBigDecimalOrNull() == null)
                messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, 6))))
        }

        return messageResults
    }
}

