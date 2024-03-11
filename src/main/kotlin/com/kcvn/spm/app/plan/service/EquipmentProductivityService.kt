package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.order.payload.response.CalendarValueResponse
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.PagingEquipmentProdResponse
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.NumberHelper.Companion.truncateDecimal
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.EquipmentProductivity
import com.kcvn.spm.repository.EquipmentProductivityRepository
import com.kcvn.spm.repository.HolidaysCalenderRepository
import com.kcvn.spm.repository.PlanRepository
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.DayOfWeek
import java.time.OffsetDateTime

@Service
@Transactional
class EquipmentProductivityService(private val equipmentProductivityRepository: EquipmentProductivityRepository,
                                   private val planRepository: PlanRepository,
                                   private val holidaysCalenderRepository: HolidaysCalenderRepository)
{

    fun getPaginatedEquipmentProductivityPlan(
        request: PlanSearchRequest?,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): PagingEquipmentProdResponse? {
        val result = PagingEquipmentProdResponse()

        val equipmentProductivity = equipmentProductivityRepository.getEquipmentProductivity()
        var startDate: OffsetDateTime? = null
        var endDate: OffsetDateTime? = null

        if (request != null) {
            if(request.startDate !=null && request.endDate!=null){
                startDate= request.startDate
                endDate = request.endDate
            }
        }
        val calendarResponses = mutableListOf<CalendarValueResponse>()
        val holidayCalender = holidaysCalenderRepository.getHolidaysCalender()

        var currentDate = startDate
        while (!currentDate!!.isAfter(endDate)) {
            val response = CalendarValueResponse(
                key = DateTimeHelper.toString(currentDate, DateTimeFormat.MM_dd),
                value = DateTimeHelper.toString(currentDate, DateTimeFormat.MM_dd),
                isHoliday = holidayCalender.any { it.toLocalDate() == currentDate!!.toLocalDate() } || currentDate.toLocalDate().dayOfWeek == DayOfWeek.SATURDAY || currentDate.toLocalDate().dayOfWeek == DayOfWeek.SUNDAY
            )
            calendarResponses.add(response)
            currentDate = currentDate.plusDays(1)
        }
        result.columns = calendarResponses

//        if (request?.orderCode != null) {
//            val plan = planRepository.getPlanByOrderCode(request.orderCode)
//            if (plan != null) {
//                startDate = plan.START_DATE
//                endDate = plan.END_DATE
//            }
//        }
        return result
    }




    fun importExcel(file: MultipartFile) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1
        var count = 0
        val total = sheet.lastRowNum - rowIndex

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val equipmentProductivity = EquipmentProductivity(
                processCode = ExcelHelper.getCellValue(row, 1).let { if (it.length > 6) it.substring(0, 6) else it },
                equipmentCode = "eCode",
                mold = ExcelHelper.getCellValue(row, 2),
                task = BigDecimal(ExcelHelper.getCellValue(row, 6)),
                time = ExcelHelper.getCellValue(row, 4).run { if (endsWith(".0")) substring(0, length - 2) else this }.toInt(),
                count = BigDecimal(ExcelHelper.getCellValue(row, 5)),
                setDay = truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 5)) *
                            BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                            BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                            BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                blockDay = truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 8)) *
                            BigDecimal(ExcelHelper.getCellValue(row, 5)) *
                            BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                            BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                            BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                blockSh = BigDecimal(ExcelHelper.getCellValue(row, 8)),
                frame_1 = ExcelHelper.getCellValue(row, 0),
                sheetHour = truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                            BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                sheetDay = truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                            BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                            BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                operatingRate = truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 3)) * BigDecimal(100)
                ),
                sheetHour_100 = truncateDecimal(BigDecimal(ExcelHelper.getCellValue(row, 7))),
                description = "insert"
            )

            equipmentProductivityRepository.add(equipmentProductivity)
            count++
        }
        return BaseResponse(null, CommonUtils.getMessage("Insert Ok", arrayOf(count, total + 1)))
    }



}