package com.kcvn.spm.app.order.service

import com.kcvn.spm.app.order.payload.model.CheckWorkResultModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.payload.response.CalendarValueResponse
import com.kcvn.spm.app.order.payload.response.OrderCodeResponse
import com.kcvn.spm.app.order.payload.response.PagingOrderResponse
import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.OrderFilterType
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Order
import com.kcvn.spm.model.tables.pojos.OrderDetail
import com.kcvn.spm.repository.*
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

@Service
@Transactional
class OrderService(
    private val orderRep: OrderRepository,
    private val workResultRep: WorkResultRepository,
    private val productRep: ProductRepository,
    private val orderDetailRep: OrderDetailRepository,
    private val holidaysCalenderRepository: HolidaysCalenderRepository,
) {

    fun getPaginatedOrder(
        request: OrderSearchRequest?,
        pageable: Pageable?
    ): PagingOrderResponse {
        val holidayCalender = holidaysCalenderRepository.getHolidaysCalender();
        val calendarResponses = mutableListOf<CalendarValueResponse>()
        if (request != null) {
            var colStartDate = OffsetDateTime.now()
            var colEndDate= OffsetDateTime.now()
            if (request.filterType == OrderFilterType.ORDER && request.orderCode != null) {

                val versionArray = request.version?.split(",")
                var minStartDate: OffsetDateTime? = null
                var maxEndDate: OffsetDateTime? = null
                val versions = versionArray?.mapNotNull { it.trim().toIntOrNull() }
                val orders = orderRep.getOrdersByCodeAndVersions(request.orderCode!!, versions)
                if (orders.isNotEmpty())
                    for (order in orders) {
                        if (minStartDate == null || order.startDate?.isBefore(minStartDate) == true) {
                            minStartDate = order.startDate
                        }
                        if (maxEndDate == null || order.endDate?.isAfter(maxEndDate) == true) {
                            maxEndDate = order.endDate
                        }
                    }

                colStartDate = DateTimeHelper.toTimeZone7(minStartDate)
                colEndDate = DateTimeHelper.toTimeZone7(maxEndDate)
            }
            if (request.filterType == OrderFilterType.DATE && request.startDate != null && request.endDate != null) {
                colStartDate = DateTimeHelper.toTimeZone7(request.startDate)
                colEndDate = DateTimeHelper.toTimeZone7(request.endDate)
            }

            var currentDate = colStartDate
            while (!currentDate!!.isAfter(colEndDate)) {
                val response = CalendarValueResponse(
                    key = DateTimeHelper.toString(currentDate, DateTimeFormat.MM_dd_yyyy),
                    value = DateTimeHelper.toString(currentDate, DateTimeFormat.MM_dd),
                    isHoliday = holidayCalender.any { it.toLocalDate() == currentDate.toLocalDate() }
                )
                calendarResponses.add(response)
                currentDate = currentDate.plusDays(1)
            }
        }

        val response = PagingOrderResponse()
        response.columns = calendarResponses
        val orderDetails = orderRep.getPagingListOrder(request, pageable)

        val orderProductIds = orderDetails.first.map { x -> Pair(x.orderId!!, x.productId!!) }
        val quantityByCalendars = orderRep.getQuantityByCalendar(orderProductIds)

        response.data = orderDetails.first.map { model ->
            val quantityByCalendar = quantityByCalendars.filter { m -> m.orderId == model.orderId && m.productId == model.productId }
                .map { m -> KeyValueResponse(m.orderDate, m.quantity.toString()) }
            model.quantityByCalendars = quantityByCalendar
            model.version = "v${model.version}.0"
            if (!model.productName.isNullOrEmpty()) {
                model.productShortcutName = model.productName!!.substring(model.productName!!.length - 7, model.productName!!.length)
            }
            model
        }

        response.totalRecords = orderDetails.second
        return response
    }

    fun exportOrderExcel(request: OrderSearchRequest?, pageable: Pageable): BaseResponse<FileContentModel> {
        val listOrderResponse = getPaginatedOrder(request, pageable)
        val fileTemplate =
            File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportOrderTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (listOrderResponse.columns != null) {
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
            val rowNumber = 0
            val columnNumber = 9
            val dataRow: Row = sheet.getRow(rowNumber) ?: sheet.createRow(rowNumber)

            val keyValueList: MutableList<CalendarValueResponse> = mutableListOf()

            for ((index, column) in listOrderResponse.columns!!.withIndex()) {

                val cell = dataRow.createCell(columnNumber + index)
                cell.setCellValue(column.key)
                val cellStyle: CellStyle = workbook.createCellStyle()
                cellStyle.cloneStyleFrom(style)
                if (column.isHoliday) {
                    cellStyle.fillForegroundColor = IndexedColors.PINK.index
                } else {
                    cellStyle.fillForegroundColor = IndexedColors.LIGHT_GREEN.index
                }

                cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

                cell.cellStyle = cellStyle
                val indexColumn = (columnNumber + index).toString()
                keyValueList.add(CalendarValueResponse(column.key, indexColumn, column.isHoliday))
            }

            val listOrder = listOrderResponse.data

            var rowNumberFill = 1
            if (listOrder != null) {
                for (item in listOrder) {
                    val row: Row = sheet.createRow(rowNumberFill++)
                    row.createCell(0).setCellValue(item.productShortcutName)
                    row.getCell(0).cellStyle = style

                    row.createCell(1).setCellValue(item.productName)
                    row.getCell(1).cellStyle = style

                    row.createCell(2).setCellValue(item.quantity.toString())
                    row.getCell(2).cellStyle = style

                    row.createCell(3).setCellValue(item.frame_1)
                    row.getCell(3).cellStyle = style

                    row.createCell(4).setCellValue(item.pcsSh.toString())
                    row.getCell(4).cellStyle = style

                    row.createCell(5).setCellValue(item.shBlock.toString())
                    row.getCell(5).cellStyle = style

                    row.createCell(6).setCellValue(item.shBlock.toString())
                    row.getCell(6).cellStyle = style

                    row.createCell(7).setCellValue(item.srNosr)
                    row.getCell(7).cellStyle = style

                    row.createCell(8).setCellValue(item.version)
                    row.getCell(8).cellStyle = style

                    for (orderDetail in item.quantityByCalendars!!) {
                        val check = keyValueList.find { x -> x.key == orderDetail.key }
                        if (check != null) {
                            check.value?.let { row.createCell(it.toInt()).setCellValue(orderDetail.value) }
                            check.value?.let {
                                val cell = row.getCell(it.toInt())
                                cell?.cellStyle = style
                            }
                        }

                    }
                }
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        val excelBytes = byteArrayOutputStream.toByteArray()
        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportOrder", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = Constants.EXCEL_CONTENT_TYPE,
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

    fun getOrderCodeByMonth(request: CalculateQuantityRequest): List<Order> {
        val orders = orderRep.getOrderCode(request.startDate,request.endDate)

        return orders
    }

    fun importExcelOrder(file: MultipartFile, orderCodeSelected: String?): BaseResponse<FileContentModel> {
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
            val orderCode = "${DateTimeHelper.toString(startDate, DateTimeFormat.dd_MM_yyyy)}-${DateTimeHelper.toString(endDate, DateTimeFormat.dd_MM_yyyy)}"
            var startDateUtc = DateTimeHelper.toUniversalTime(startDate)
            val endDateUtc = DateTimeHelper.toUniversalTime(endDate)
            var version = 1

            if (startDate > endDate)
                throw BusinessException(CommonUtils.getMessage("validate.excel.startDate.gt.endDate"))

            if (orderCodeSelected.isNullOrEmpty()) {
                if (Duration.between(endDate, startDate).toDays() > 31)
                    throw BusinessException(CommonUtils.getMessage("validate.excel.column.invalidDiffDate", arrayOf(31)))

                if (startDate < DateTimeHelper.getFirstDayOfQuarterInYear(LocalDateTime.now()))
                    throw BusinessException(CommonUtils.getMessage("validate.excel.column.quarterInYear"))

                val overlapOrder = orderRep.getOverlapOrderDate(startDateUtc, endDateUtc)
                if (overlapOrder != null)
                    throw BusinessException(CommonUtils.getMessage("validate.excel.orderOverlap"))

            } else {
                val orderExist = orderRep.getByOrderCode(orderCodeSelected)
                    ?: throw BusinessException(CommonUtils.getMessage("validate.orderNotExist"))

                if (startDateUtc < orderExist.startDate || endDateUtc > orderExist.endDate)
                    throw BusinessException(CommonUtils.getMessage("validate.excel.invalidTime"))

                val workResult = workResultRep.getMaxByDate(startDateUtc, endDateUtc)
                if (workResult?.summaryResultDate != null) {
                    if (workResult.summaryResultDate!! >= endDateUtc)
                        throw BusinessException(CommonUtils.getMessage("validate.order.hasWorkResult"))

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
                    isBreak = true
                    check = false
                    messageResults.add(CommonUtils.getMessage("product.not.exist"))
                }

                if (check) {
                    var isValidCol = true
                    for (iCol in 1 until colIndexResult) {
                        try {
                            val arrOrderDate = ExcelHelper.getCellValue(headerRow, iCol, DateTimeFormat.MM_dd).split("/")
                            val orderDate = LocalDateTime.of(year, arrOrderDate[0].toInt(), arrOrderDate[1].toInt(), 0, 0)
                            val strQuantity = ExcelHelper.getCellValue(row, iCol)
                            if (strQuantity.isEmpty()) {
                                continue
                            } else {
                                if (strQuantity.toBigDecimalOrNull() == null) {
                                    isBreak = true
                                    isValidCol = false
                                    messageResults.add(CommonUtils.getMessage("validate.excel.isNumber", arrayOf(ExcelHelper.getCellValue(headerRow, iCol))))
                                    break
                                }
                            }
                            val orderDetail = OrderDetail(
                                productId = product!!.id,
                                orderDate = OffsetDateTime.of(orderDate, ZoneOffset.UTC),
                                quantity = strQuantity.toBigDecimalOrNull()?.toInt()
                            )
                            orderDetails.add(orderDetail)
                        } catch (e: Exception) {
                            isBreak = true
                            isValidCol = false
                            messageResults.add(CommonUtils.getMessage("validate.excel.updateDataError"))
                        }
                    }
                    if (isValidCol) {
                        productImports.add(name)
                        messageResults.add(CommonUtils.getMessage("validate.excel.checked"))
                        count++
                    }
                }

                val result = messageResults.joinToString(separator = "; ")

                if (row.getCell(colIndexResult) == null) {
                    row.createCell(colIndexResult)
                }
                row.getCell(colIndexResult).setCellValue(result)
                val hasFontColor = result == CommonUtils.getMessage("validate.excel.checked")
                row.getCell(colIndexResult).cellStyle = ExcelHelper.getCellStyleResultCol(workbook, style, hasFontColor)
            }

            if (!isBreak) {
                try {
                    val order = Order(
                        orderCode = orderCodeSelected ?: orderCode,
                        startDate = startDateUtc,
                        endDate = endDateUtc,
                        version = version
                    )

                    orderRep.addOrder(order, orderDetails)

                    return BaseResponse(null, CommonUtils.getMessage("import.success", arrayOf(count, total)))

                } catch (e: Exception) {
                    throw e
                }
            }

            val byteArrayOutputStream = ByteArrayOutputStream()
            workbook.write(byteArrayOutputStream)

            val excelBytes = byteArrayOutputStream.toByteArray()

            val response = FileContentModel(
                fileName = CommonUtils.getMessage("fileName.resultImportOrder", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern(DateTimeFormat.yyyy_MM_dd_HH_mm_ss)))),
                contentType = Constants.EXCEL_CONTENT_TYPE,
                content = excelBytes
            )

            workbook.close()

            return BaseResponse(response, CommonUtils.getMessage("import.insertNoData"))
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
            contentType = Constants.EXCEL_CONTENT_TYPE,
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

    fun getOrderDetailsByOrderIds(ids: List<String?>): List<OrderDetail> {
        return orderDetailRep.getOrderDetailsByOrderIds(ids)
    }
}