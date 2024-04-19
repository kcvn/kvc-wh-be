package com.kcvn.spm.app.order.service

import com.kcvn.spm.app.order.payload.model.CheckWorkResultModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.payload.response.OrderCodeResponse
import com.kcvn.spm.app.order.payload.response.PagingOrderResponse
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.OrderVersion
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.StringHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.OrderInfo
import com.kcvn.spm.repository.HolidaysCalenderRepository
import com.kcvn.spm.repository.OrderInfoRepository
import com.kcvn.spm.repository.OrderRepository
import com.kcvn.spm.repository.ProductRepository
import com.kcvn.spm.repository.WorkResultRepository
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.DateUtil
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
import java.io.File
import java.io.FileInputStream
import java.time.Duration
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
@Transactional
class OrderService(
    private val orderRep: OrderRepository,
    private val workResultRep: WorkResultRepository,
    private val productRep: ProductRepository,
    private val holidaysCalenderRep: HolidaysCalenderRepository,
    private val orderInfoRep: OrderInfoRepository
) {

    fun getPaginatedOrder(request: OrderSearchRequest, pageable: Pageable, isExport: Boolean = false): PagingOrderResponse {
        val response = PagingOrderResponse()

        if (request.startDate != null && request.endDate != null) {
            val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
            response.columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(request.startDate)!!, DateTimeHelper.toTimeZone7(request.endDate)!!, holidayCalenders)
        }

        val orderDetails = orderInfoRep.getPagingListOrder(request, pageable, isExport)

        val productVersion = orderDetails.first.map { x -> Pair(x.productName!!, x.version ?: "") }
        val quantityByCalendars = orderInfoRep.getQuantityByCalendar(
            productVersion,
            request.startDate,
            request.endDate,
            (request.version == OrderVersion.LATEST)
        )

        response.data = orderDetails.first.map { model ->
            val quantityByCalendar = quantityByCalendars.filter {
                m -> m.productName == model.productName && (request.version == OrderVersion.LATEST || (m.version ?: "") == (model.version ?: ""))
            }

            model.quantityByCalendars = quantityByCalendar.map { m -> KeyValueResponse(m.orderDate, m.quantity.toString(), isHasDifferent = m.isHasDifferent) }

            if (request.version ==OrderVersion.LATEST) model.version = quantityByCalendar.firstOrNull()?.version
            if (!model.version.isNullOrEmpty()) {
                model.version = "V${StringHelper.intToStringD2(model.version)}"
            }
            if (!model.productName.isNullOrEmpty()) {
                model.productShortcutName = model.productName!!.substring(model.productName!!.length - 7, model.productName!!.length)
            }
            model
        }

        response.totalRecords = orderDetails.second
        return response
    }

    fun getOrderVersionDropdown(): BaseResponse<List<DropdownResponse>> {
        val data = orderInfoRep.getOrderVersionDropdown()
            .map { x -> DropdownResponse(x.version, x.label) }
            .toMutableList()
        data.add(0, OrderVersion.DEFAULT)
        return BaseResponse(data)
    }

    fun exportOrderExcel(request: OrderSearchRequest, pageable: Pageable): BaseResponse<FileContentModel> {
        val listOrderResponse = getPaginatedOrder(request, pageable, true)
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportOrderTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        val rowNumber = 0
        val dataRow: Row = sheet.getRow(rowNumber) ?: sheet.createRow(rowNumber)
        val style = ExcelHelper.getCellStyleCommon(workbook)
        val headerStyle = dataRow.getCell(0).cellStyle

        if (listOrderResponse.columns.isNotEmpty()) {
            var headerCol = 9
            for (col in listOrderResponse.columns) {
                ExcelHelper.setCellValueWithCalendar(workbook, dataRow, headerCol, headerStyle, col.value, col.isHoliday)
                headerCol++
            }
        }

        val listOrder = listOrderResponse.data

        var rowNumberFill = 1
        if (listOrder != null) {
            for (item in listOrder) {
                val row: Row = sheet.createRow(rowNumberFill++)
                ExcelHelper.setCellValue(row, 0, style, item.productShortcutName)
                ExcelHelper.setCellValue(row, 1, style, item.productName)
                ExcelHelper.setCellValue(row, 2, style, item.quantity?.toString())
                ExcelHelper.setCellValue(row, 3, style, item.frame_1)
                ExcelHelper.setCellValue(row, 4, style, item.layerCount?.toString())
                ExcelHelper.setCellValue(row, 5, style, item.pcsSh?.toString())
                ExcelHelper.setCellValue(row, 6, style, item.shBlock?.toString())
                ExcelHelper.setCellValue(row, 7, style, item.srNosr)
                ExcelHelper.setCellValue(row, 8, style, item.version)

                if (listOrderResponse.columns.isNotEmpty()) {
                    var colIndex = 9
                    for (col in listOrderResponse.columns) {
                        val orderDetail = item.quantityByCalendars?.find { it.key == col.key }
                        ExcelHelper.setCellValueWithCalendar(workbook, row, colIndex, style, orderDetail?.value, col.isHoliday)
                        colIndex++
                    }
                }

            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        val excelBytes = byteArrayOutputStream.toByteArray()
        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportOrder", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

    fun getOrderCode(year: String?): List<OrderCodeResponse> {
        var startDate = OffsetDateTime.of(DateTimeHelper.getFirstDayOfQuarterInYear(LocalDateTime.now()), ZoneOffset.UTC)
        var endDate: OffsetDateTime? = null
        if (!year.isNullOrEmpty()) {
            startDate = OffsetDateTime.of(year.toInt(), 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)
            endDate = OffsetDateTime.of(year.toInt(), 12, 31, 23, 59, 59, 0, ZoneOffset.UTC)
        }
        val orders = orderRep.getOrderCode(startDate, endDate)

        val versionMap = mutableMapOf<String, MutableList<String>>()
        for (order in orders) {
            val orderCode = order.orderCode
            val version = order.version
            if (orderCode != null) {
                versionMap.computeIfAbsent(orderCode) { mutableListOf() }.add(version.toString())
            }
        }

        val orderCodeResponses = mutableListOf<OrderCodeResponse>()
        for ((orderCode, versions) in versionMap) {
            var dropdownResponses = versions.map { DropdownResponse(it, "v${it}.0") }.sortedByDescending { x -> x.value }
            if (year.isNullOrEmpty()) dropdownResponses = listOf(dropdownResponses.first())
            orderCodeResponses.add(OrderCodeResponse(orderCode, orderCode, dropdownResponses))
        }

        return orderCodeResponses
    }

    fun importExcelOrder(file: MultipartFile, isIncreaseVersion: Boolean?): BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1

            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))

            val headerRow = sheet.getRow(0)
                ?: throw BusinessException(CommonUtils.getMessage("validate.excel.headerInFirstRow"))
            val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportOrderTemplate.xlsx"

            val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)

            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 1))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

            val formatDates = arrayOf("MM/dd", "M/d", "M/dd", "MM/dd/yyyy", "M/d/yyyy", "M/dd/yyyy")
            if (!ExcelHelper.checkCalendarColumn(headerRow, 1, colIndexResult - 1, formatDates))
                throw BusinessException(CommonUtils.getMessage("validate.excel.column.invalidCalendar"))

            if (!checkContinuousDate(headerRow, 1, colIndexResult - 1))
                throw BusinessException(CommonUtils.getMessage("validate.excel.date.notContinuous"))

            val year = LocalDateTime.now().year
            val arrStartDate = ExcelHelper.getCellValue(headerRow, 1, DateTimeFormat.MM_dd).split("/")
            val startDate = LocalDateTime.of(year, arrStartDate[0].toInt(), arrStartDate[1].toInt(), 0, 0)
            val arrEndDate = ExcelHelper.getCellValue(headerRow, colIndexResult - 1, DateTimeFormat.MM_dd).split("/")
            val endDate = LocalDateTime.of(year, arrEndDate[0].toInt(), arrEndDate[1].toInt(), 0, 0)
            val startDateUtc = DateTimeHelper.toUniversalTime(startDate)
            val endDateUtc = DateTimeHelper.toUniversalTime(endDate)

            if (startDate > endDate)
                throw BusinessException(CommonUtils.getMessage("validate.excel.startDate.gt.endDate"))

            if ((Duration.between(startDate, endDate).toDays() + 1)  > 65)
                throw BusinessException(CommonUtils.getMessage("validate.excel.column.invalidDiffDate", arrayOf(65)))

            val productNames = sheet.filter { x -> x.rowNum >= rowIndex }.mapNotNull { row -> ExcelHelper.getCellValue(row, 0) }
            val productExists = productRep.getByName(productNames)

            val orderDetails = mutableListOf<OrderInfo>()
            val productImports = mutableListOf<String>()
            var count = 0
            var total = 0
            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val name = ExcelHelper.getCellValue(row, 0)
                if (name.isEmpty()) break
                val style = row.getCell(0)?.cellStyle ?: break
                val messageResults = mutableListOf<String>()
                total++
                var check = true
                if (productImports.any { x -> x == name }) {
                    check = false
                    messageResults.add(CommonUtils.getMessage("validate.excel.duplicate"))
                }
                val product = productExists.find { x -> x.name == name }
                if (product == null) {
                    check = false
                    messageResults.add(CommonUtils.getMessage("product.not.exist"))
                }

                if (check) {
                    var isValidCol = true
                    var countCellEmpty = 0
                    for (iCol in 1 until colIndexResult) {
                        try {
                            val arrOrderDate = ExcelHelper.getCellValue(headerRow, iCol, DateTimeFormat.MM_dd).split("/")
                            val orderDate = LocalDateTime.of(year, arrOrderDate[0].toInt(), arrOrderDate[1].toInt(), 0, 0)
                            if (orderDate < startDate) continue

                            val strQuantity = ExcelHelper.getCellValue(row, iCol)
                            if (strQuantity.isEmpty()) {
                                countCellEmpty++
                                continue
                            } else {
                                if (strQuantity.toBigDecimalOrNull() == null) {
                                    isValidCol = false
                                    messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, iCol, DateTimeFormat.MM_dd))))
                                    break
                                }
                            }
                            val orderDetail = OrderInfo(
                                productName = product!!.name,
                                frame_1 = product.frame_1,
                                layerCount = product.layerCount,
                                pcsSh = product.pcsSh,
                                blockSh = product.shBlock,
                                srNosr = product.srNosr,
                                version = 0,
                                orderDate = DateTimeHelper.toUniversalTime(orderDate),
                                quantity = strQuantity.toBigDecimalOrNull()?.toInt()
                            )
                            orderDetails.add(orderDetail)
                        } catch (e: Exception) {
                            isValidCol = false
                            messageResults.add(CommonUtils.getMessage("validate.excel.updateDataError"))
                        }
                    }
                    if (countCellEmpty >= colIndexResult - 1) {
                        messageResults.add(CommonUtils.getMessage("validate.excel.rowEmpty"))
                    }
                    else {
                        if (isValidCol) {
                            productImports.add(name)
                            messageResults.add(CommonUtils.getMessage("validate.excel.importSuccess"))
                            count++
                        }
                    }
                }

                val result = messageResults.joinToString(separator = "; ")

                if (row.getCell(colIndexResult) == null) {
                    row.createCell(colIndexResult)
                }
                row.getCell(colIndexResult).setCellValue(result)
                row.getCell(colIndexResult).cellStyle = ExcelHelper.getCellStyleResultCol(workbook, style)
            }

            orderInfoRep.addOrderInfo(orderDetails, isIncreaseVersion ?: false, startDateUtc, endDateUtc)
            if (count == total) {
                return BaseResponse(null, CommonUtils.getMessage("import.success", arrayOf(count, total)))
            }

            val errorRows = sheet.filter {
                x -> ExcelHelper.getCellValue(x, colIndexResult) != CommonUtils.getMessage("validate.excel.importSuccess")
            }

            val response = exportFileError(errorRows, workbook, sheet)

            workbook.close()

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

    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportOrderTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importOrderTemplate"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }

    fun checkWorkResult(orderCode: String): BaseResponse<CheckWorkResultModel> {
        val orderExist = orderRep.getByOrderCode(orderCode)
            ?: throw BusinessException(CommonUtils.getMessage("validate.orderNotExist"))

        var hasWorkResult = false
        val workResult = workResultRep.getMaxByDate(orderExist.startDate!!, orderExist.endDate!!)
        if (workResult?.summaryResultDate != null) {
            hasWorkResult = true
        }
        val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.dd_MM_yyyy)
        return BaseResponse(CheckWorkResultModel(
            hasWorkResult,
            if (hasWorkResult) CommonUtils.getMessage("import.order.messageCheckWorkResult", arrayOf(workResult?.summaryResultDate!!.format(formatter))) else null
        ))
    }

    private fun checkContinuousDate(headerRow: Row, startCol: Int, endCol: Int): Boolean {
        val days = headerRow.filter { x -> x.columnIndex in startCol..endCol }
            .mapNotNull { x ->
                LocalDateTime.of(
                    LocalDateTime.now().year,
                    ExcelHelper.getCellValue(headerRow, x.columnIndex, DateTimeFormat.MM_dd).split("/")[0].toInt(),
                    ExcelHelper.getCellValue(headerRow, x.columnIndex, DateTimeFormat.MM_dd).split("/")[1].toInt(),
                    0, 0
                )
            }
        for (i in days.indices) {
            if (i == 0) continue
            val formatter = DateTimeFormatter.ofPattern(DateTimeFormat.yyyyMMdd)
            if (days[i - 1].plusDays(1).format(formatter) != days[i].format(formatter)) return false
        }
        return true
    }

    private fun exportFileError(dataRows: List<Row>, workbook: Workbook, importSheet: Sheet): FileContentModel {
        val sheet = workbook.createSheet()

        for ((rowNumber, dataRow) in dataRows.withIndex()) {
            val row = sheet.createRow(rowNumber)
            row.height = dataRow.height
            for (colIndex in 0 until dataRow.lastCellNum) {
                if (rowNumber == 0) {
                    sheet.setColumnWidth(colIndex, importSheet.getColumnWidth(colIndex))
                }
                val cell = dataRow.getCell(colIndex)
                val style = cell.cellStyle
                if (cell.cellType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    ExcelHelper.setCellValue(row, colIndex, style, cell.dateCellValue)
                }
                else {
                    var value = ExcelHelper.getCellValue(dataRow, colIndex)
                    if (value.toBigDecimalOrNull() != null) {
                        value = value.toBigDecimal().toInt().toString()
                    }
                    ExcelHelper.setCellValue(row, colIndex, style, value)
                }

            }
        }

        workbook.removeSheetAt(0)
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.resultImportOrder", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return response
    }
}