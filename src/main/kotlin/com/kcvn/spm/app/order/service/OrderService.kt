package com.kcvn.spm.app.order.service

import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.payload.response.CalendarValueResponse
import com.kcvn.spm.app.order.payload.response.PagingOrderResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.OrderDetailRepository
import com.kcvn.spm.repository.OrderRepository
import org.apache.poi.ss.usermodel.*
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
                    cellStyle.fillForegroundColor  = IndexedColors.PINK.index
                } else {
                    cellStyle.fillForegroundColor  = IndexedColors.LIGHT_GREEN.index
                }

                cellStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

                cell.cellStyle = cellStyle
                val indexColumn = (columnNumber + index).toString()
                keyValueList.add(CalendarValueResponse(column.key,indexColumn,column.isHoliday))
            }

            val listOrder =listOrderResponse.data

            var rowNumberFill = 1
            if(listOrder !=null){
                for (item in listOrder) {
                    val dataRow: Row = sheet.createRow(rowNumberFill++)
                    dataRow.createCell(0).setCellValue(item.productShortcutName)
                    dataRow.getCell(0).cellStyle = style

                    dataRow.createCell(1).setCellValue(item.productName)
                    dataRow.getCell(1).cellStyle = style

                    dataRow.createCell(2).setCellValue(item.quantity.toString())
                    dataRow.getCell(2).cellStyle = style

                    dataRow.createCell(3).setCellValue(item.frame_1)
                    dataRow.getCell(3).cellStyle = style

                    dataRow.createCell(4).setCellValue(item.pcsSh.toString())
                    dataRow.getCell(4).cellStyle = style

                    dataRow.createCell(5).setCellValue(item.shBlock.toString())
                    dataRow.getCell(5).cellStyle = style

                    dataRow.createCell(6).setCellValue(item.shBlock.toString())
                    dataRow.getCell(6).cellStyle = style

                    dataRow.createCell(7).setCellValue(item.srNosr)
                    dataRow.getCell(7).cellStyle = style

                    dataRow.createCell(8).setCellValue("v"+item.version+".0")
                    dataRow.getCell(8).cellStyle = style

                    for(odetail in item.quantityByCalendars!!){


                        val check = keyValueList.find { x-> x.key == odetail.key }
                        if(check !=null){
                            check.value?.let { dataRow.createCell(it.toInt()).setCellValue(odetail.value) }
                            check.value?.let {
                                val cell = dataRow.getCell(it.toInt())
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
            fileName = CommonUtils.getMessage("fileName.exportCompletionRateProductProcess", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = Constants.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

}