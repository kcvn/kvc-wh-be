package com.kcvn.spm.app.order.service

import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.payload.response.CalendarValueResponse
import com.kcvn.spm.app.order.payload.response.PagingOrderResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.OrderDetailRepository
import com.kcvn.spm.repository.OrderRepository
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.time.DayOfWeek
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class OrderService (
    private val orderRep: OrderRepository,
    private val orderDetailRep: OrderDetailRepository
) {
    fun getPaginatedCompletionRateProduct(
        request: OrderSearchRequest?,
        pageable: Pageable?
    ): PagingOrderResponse
    {
        val calendarResponses = mutableListOf<CalendarValueResponse>()

        if (request?.startDate != null && request.endDate != null) {

            var currentDate = request.startDate
            while (!currentDate!!.isAfter(request.endDate)) {
                val response = CalendarValueResponse(
                    key = currentDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                    value = currentDate.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                    isHoliday = currentDate.dayOfWeek == DayOfWeek.SATURDAY || currentDate.dayOfWeek == DayOfWeek.SUNDAY
                )
                calendarResponses.add(response)

                currentDate = currentDate.plusDays(1)
            }
        }

        val pagingOrderResponse = PagingOrderResponse()
        pagingOrderResponse.columns = calendarResponses

        val listOrderResponse = orderRep.getPaginatedCompletionRateProduct(request,pageable)
        pagingOrderResponse.data = listOrderResponse.first
        pagingOrderResponse.totalRecords = listOrderResponse.second



        return pagingOrderResponse

    }



    fun exportOrderExcel(request: OrderSearchRequest?, pageable: Pageable) : BaseResponse<FileContentModel> {
        val listOrderResponse = getPaginatedCompletionRateProduct(request,pageable)
        val check = listOrderResponse.columns
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportOrderTemplate.xlsx")
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
            var rowNumber = 0
            var columnNumber = 9
            val dataRow: Row = sheet.createRow(rowNumber)
            for ((index, column) in listOrderResponse.columns!!.withIndex()) {
                val cell = dataRow.createCell(columnNumber + index)
                cell.setCellValue(column.key)
                cell.cellStyle = style
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportCompletionRateProductProcess", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = Constants.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

}