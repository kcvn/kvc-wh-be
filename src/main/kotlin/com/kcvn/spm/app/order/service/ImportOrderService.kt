package com.kcvn.spm.app.order.service

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.datetimehelper.DateTimeHelper
import com.kcvn.spm.common.helper.excelhelper.ExcelHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Order
import com.kcvn.spm.model.tables.pojos.OrderDetail
import com.kcvn.spm.repository.OrderRepository
import com.kcvn.spm.repository.ProductRepository
import com.kcvn.spm.repository.WorkResultRepository
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
@Transactional
class ImportOrderService (
    private val orderRep: OrderRepository,
    private val productRep: ProductRepository,
    private val workResultRep: WorkResultRepository
) {
    fun importExcelOrder(file: MultipartFile, orderCodeSelected: String?) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex)) {
            workbook.close()
            throw BusinessException(CommonUtils.getMessage("import.file.empty"))
        }

        val headerRow = sheet.getRow(0)
        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportOrderTemplate.xlsx"

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

        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 1)){
            workbook.close()
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))
        }

        val formatDates = arrayOf("MM/dd", "M/d", "M/dd", "MM/dd/yyyy", "M/d/yyyy", "M/dd/yyyy")
        if (!ExcelHelper.checkCalendarColumn(headerRow, 1, colIndexResult - 1, formatDates)){
            workbook.close()
            throw BusinessException(CommonUtils.getMessage("validate.excel.column.invalidCalendar"))
        }

        if (checkContinuousDate(headerRow, 1, colIndexResult - 1)) {
            workbook.close()
            throw BusinessException(CommonUtils.getMessage("validate.excel.date.notContinuous"))
        }

        val year = LocalDateTime.now().year
        val arrStartDate = ExcelHelper.getCellValue(headerRow, 1).split("/")
        val startDate = LocalDateTime.of(year, arrStartDate[0].toInt(), arrStartDate[1].toInt(), 0, 0)
        val arrEndDate = ExcelHelper.getCellValue(headerRow, colIndexResult - 1).split("/")
        val endDate = LocalDateTime.of(year, arrEndDate[0].toInt(), arrEndDate[1].toInt(), 0, 0)
        val orderCode = "${startDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}-${endDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))}"
        var startDateUtc = OffsetDateTime.of(startDate, ZoneOffset.UTC)
        val endDateUtc = OffsetDateTime.of(endDate, ZoneOffset.UTC)
        var version = 1

        if (startDate > endDate) {
            workbook.close()
            throw BusinessException(CommonUtils.getMessage("validate.excel.startDate.gt.endDate"))
        }

        if (orderCodeSelected.isNullOrEmpty()) {
            if (Duration.between(endDate, startDate).toDays() > 31) {
                workbook.close()
                throw BusinessException(CommonUtils.getMessage("validate.excel.column.invalidDiffDate", arrayOf(31)))
            }
            if (startDate < DateTimeHelper.getFirstDayOfQuarterInYear(LocalDateTime.now())) {
                workbook.close()
                throw BusinessException(CommonUtils.getMessage("validate.excel.column.quarterInYear"))
            }
            val overlapOrder = orderRep.getOverlapOrderDate(startDateUtc, endDateUtc, orderCode)
            if (overlapOrder != null) {
                workbook.close()
                throw BusinessException(CommonUtils.getMessage("validate.excel.orderOverlap"))
            }
        }
        else {
            val orderExist = orderRep.getByOrderCode(orderCodeSelected)
            if (orderExist == null) {
                workbook.close()
                throw BusinessException(CommonUtils.getMessage("validate.orderNotExist"))
            }
            if (startDateUtc < orderExist.startDate || endDateUtc > orderExist.endDate) {
                workbook.close()
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidTime"))
            }

            val workResult = workResultRep.getMaxByDate(startDateUtc, endDateUtc)
            if (workResult?.summaryResultDate != null){
                if (workResult.summaryResultDate!! >= endDateUtc) {
                    workbook.close()
                    throw BusinessException(CommonUtils.getMessage("validate.order.hasWorkResult"))
                }
                val workResultDate = workResult.summaryResultDate!!.plusDays(1)
                startDateUtc = OffsetDateTime.of(year, workResultDate.monthValue, workResultDate.dayOfMonth, 0, 0, 0, 0, ZoneOffset.UTC)
            }
            version = (orderExist.version ?: 0) + 1
        }

        val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
        val productExists = productRep.getByName(productNames)

        val orderDetails = mutableListOf<OrderDetail>()
        val productImports = mutableListOf<String>()
        var isBreak = false
        var count = 0
        val total = sheet.filter { x -> x.rowNum >= rowIndex }.size
        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val messageResults = mutableListOf<String>()
            val style = row.getCell(0).cellStyle
            val name = ExcelHelper.getCellValue(row, 0)
            var check = true
            if (productImports.any { x -> x == name}) {
                check = false
                messageResults.add(CommonUtils.getMessage("validate.excel.duplicate"))
            }
            val product = productExists.find { x -> x.name == name }
            if (product == null) {
                isBreak = true
                check = false
                messageResults.add(CommonUtils.getMessage("product.not.exist"))
            }

            if (check) {
                for (iCol in 1 until colIndexResult) {
                    try {
                        val arrOrderDate = ExcelHelper.getCellValue(headerRow, iCol).split("/")
                        val orderDate = LocalDateTime.of(year, arrOrderDate[0].toInt(), arrOrderDate[1].toInt(), 0, 0)
                        if (ExcelHelper.getCellValue(row, iCol).toBigDecimalOrNull() == null) {
                            isBreak = true
                            messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, iCol))))
                            break
                        }
                        val orderDetail = OrderDetail(
                            productId = product!!.id,
                            orderDate = OffsetDateTime.of(orderDate, ZoneOffset.UTC),
                            quantity = ExcelHelper.getCellValue(row, iCol).toBigDecimalOrNull()?.toInt()
                        )
                        orderDetails.add(orderDetail)
                    } catch (e: Exception) {
                        isBreak = true
                        messageResults.add(CommonUtils.getMessage("validate.excel.updateDataError"))
                    }
                }
                if (!isBreak) {
                    productImports.add(name)
                    messageResults.add(CommonUtils.getMessage("validate.excel.importSuccess"))
                    count++
                }
            }

            val result = messageResults.joinToString(separator = "; ")

            if (row.getCell(colIndexResult) == null) {
                row.createCell(colIndexResult)
            }
            row.getCell(colIndexResult).setCellValue(result)
            row.getCell(colIndexResult).cellStyle = style

            if (isBreak) break
        }

        val order = Order(
            orderCode = orderCodeSelected ?: orderCode,
            startDate = startDateUtc,
            endDate = endDateUtc,
            version = version
        )

        orderRep.addOrder(order, orderDetails)

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.resultImportOrder", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = Constants.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(
            response,
            if(count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total))
        )
    }

    fun downloadTemplate() : BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportOrderTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importOrderTemplate"),
            contentType = Constants.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }
    private fun checkContinuousDate(headerRow: Row, startCol: Int, endCol: Int) : Boolean {
        val days = headerRow.filter { x -> x.columnIndex in startCol..endCol }
            .mapNotNull { x ->
                LocalDateTime.of(
                    LocalDateTime.now().year,
                    ExcelHelper.getCellValue(headerRow, x.columnIndex).split("/")[0].toInt(),
                    ExcelHelper.getCellValue(headerRow, x.columnIndex).split("/")[1].toInt(),
                    0, 0
                )
            }
        for (i in days.indices) {
            if (i == 0) continue
            val formatter = DateTimeFormatter.ofPattern("yyyyMMdd")
            if (days[i - 1].plusDays(1).format(formatter) != days[i].format(formatter)) return false
        }
        return true
    }
}