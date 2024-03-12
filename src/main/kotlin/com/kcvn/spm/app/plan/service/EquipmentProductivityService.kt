package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.*
import com.kcvn.spm.app.plan.payload.request.PlanProcessDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.PagingEquipmentProdResponse
import com.kcvn.spm.common.constants.*
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.NumberHelper.Companion.truncateDecimal
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.EquipmentProductivity
import com.kcvn.spm.repository.*
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.math.BigDecimal
import java.time.OffsetDateTime

@Service
@Transactional
class EquipmentProductivityService(private val equipmentProductivityRepository: EquipmentProductivityRepository,
                                   private val planRepository: PlanRepository,
                                   private val planProductRep: PlanProductRepository,
                                   private val planProcessRep: PlanProcessRepository,
                                   private val planDetailRep: PlanDetailRepository,
                                   private val holidaysCalenderRep: HolidaysCalenderRepository)
{

    fun getPaginatedEquipmentProductivityPlan(
        request: PlanSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): PagingEquipmentProdResponse? {

        val listPlan  = planRepository.getListPlan(request, pageable)

        val listPlanIds: List<String> = listPlan.first.map { plan -> plan.id.toString() }

        val requestListPlan = PlanProcessDetailRequest(listPlanIds,request.filterType,request.startDate,request.endDate,request.orderCode)

        val result = getPlanDetail(requestListPlan)


        return result

    }


    fun getPlanDetail(request: PlanProcessDetailRequest): PagingEquipmentProdResponse {
        val response = PagingEquipmentProdResponse()
        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        if (request.planProductId?.isEmpty() == true) throw BusinessException("")
        val colStartDate: OffsetDateTime
        val colEndDate: OffsetDateTime

        when (request.filterType) {
            OrderFilterType.DATE -> {
                if (request.startDate == null || request.endDate == null) throw BusinessException("")
                colStartDate = request.startDate ?: OffsetDateTime.now()
                colEndDate = request.endDate ?: OffsetDateTime.now()
            }
            OrderFilterType.ORDER -> {
                val plan = request.orderCode?.let { planRepository.getPlanByOrderCode(it) } ?: throw BusinessException("")
                colStartDate = plan.startDate ?: OffsetDateTime.now()
                colEndDate = plan.endDate ?: OffsetDateTime.now()
            }
            else -> throw BusinessException("")
        }
        response.columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(colStartDate)!!, DateTimeHelper.toTimeZone7(colEndDate)!!, holidayCalenders)
        // start here
        val plan: MutableList<EquipmentProductivityModel> = mutableListOf()

        for(planProductId in request.planProductId!!){
            val planProduct = planProductRep.getById(planProductId)
            val planProcesses = planProcessRep.getListPlanProcess(planProductId)
            val parentPlanProcess = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }
            val planProcessIds = parentPlanProcess.mapNotNull { x -> x.id }
            val planDetails = planDetailRep.getPlanDetail(planProcessIds)
            val equipmentProductList = equipmentProductivityRepository.getEquipmentProductivity()
            if(planProduct!=null){
                for(planProcess in parentPlanProcess){
                    val equipmentProductivity = EquipmentProductivityModel(
                        frame1 = planProduct.frame_1,
                        processName = planProcess.processName,
                        processNameJp= planProcess.processName,
                        processConvertCode = planProcess.processConvertCode
                    )
                    val planDetailByProcess = planDetails.filter { m -> m.planProcessId == planProcess.id }
                    val processDetailListModel =  mutableListOf<ProcessDetailListModel>()
                    //process
                    processDetailListModel.add(
                        ProcessDetailListModel(
                            type = ProcessPlan.PROCESS,
                            quantityByCalendars = planDetailByProcess.filter { t -> t.title == PlanTitle.PLAN_KEY }.map { t ->
                                KeyValueResponse(
                                    DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                                    if (planProcess.unit == ProcessUnit.BLOCK) t.blockQuantity?.toString() else t.sheetQuantity?.toString()
                                )
                            }
                        )
                    )
                    //machine
                    val uniqueSheetDayValue = equipmentProductList
                        .firstOrNull { equipment ->
                            equipment.processCode == planProcess.processCode &&
                                    equipment.mold == planProduct.mold &&
                                    equipment.frame_1 == planProduct.frame_1
                        }
                        ?.sheetDay
                    processDetailListModel.add(
                        ProcessDetailListModel(
                            type = ProcessPlan.MACHINE,
                            quantityByCalendars = response.columns!!.map { column ->
                                KeyValueResponse(
                                    column.key,
                                    uniqueSheetDayValue.toString()
                                )
                            }
                        )
                    )
                    // machine number
                    val processValues = processDetailListModel
                        .firstOrNull { it.type == ProcessPlan.PROCESS }
                        ?.quantityByCalendars
                    if (uniqueSheetDayValue != null) {
                        processDetailListModel.add(
                            ProcessDetailListModel(
                                type = ProcessPlan.MACHINENUMBER,
                                quantityByCalendars = processValues!!.map { column ->
                                    val result = (column.value?.toDoubleOrNull() ?: 0.0) / uniqueSheetDayValue.toDouble()
                                    val formattedResult = String.format("%.1f", result)
                                    KeyValueResponse(
                                        column.key,
                                        formattedResult
                                    )
                                }
                            )
                        )
                    }
                    //end
                    val totalProcessValue: Int = processDetailListModel.sumOf { processDetail ->
                        processDetail.quantityByCalendars.sumOf { keyValueResponse ->
                            keyValueResponse.value?.toIntOrNull() ?: 0
                        }
                    }
                    val processDetailList = mutableListOf<ProcessDetailModel>()
                    processDetailList.add(
                        ProcessDetailModel(
                            name=planProduct.mold,
                            totalProcess = totalProcessValue,
                            processDetailList = processDetailListModel
                        )
                    )
                    equipmentProductivity.processDetail = processDetailList
                    plan.add(equipmentProductivity)
                }
            }
        }


        val groupedData = plan.groupBy { it ->
            listOf(
                it.frame1,
                it.processName,
                it.processNameJp,
                it.processConvertCode
            )
        }

        val mergedData = groupedData.values.map { group ->
            val mergedProcessDetail = group.flatMap { it.processDetail!! }
            group.first().copy(processDetail = mergedProcessDetail)
        }


        response.data = mergedData
        return response
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