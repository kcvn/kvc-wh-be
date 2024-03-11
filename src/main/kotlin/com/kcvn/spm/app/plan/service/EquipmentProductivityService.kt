package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.PlanDataByProcessModel
import com.kcvn.spm.app.plan.payload.model.ProcessChildrenModel
import com.kcvn.spm.app.plan.payload.model.ProductPlanDetailModel
import com.kcvn.spm.app.plan.payload.request.PlanDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.PagingEquipmentProdResponse
import com.kcvn.spm.app.plan.payload.response.ProductPlanDetailResponse
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
                                   private val workResultRep: WorkResultRepository)
{

    fun getPaginatedEquipmentProductivityPlan(
        request: PlanSearchRequest?,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): PagingEquipmentProdResponse? {
        val result = PagingEquipmentProdResponse()
        val equipmentProductivity = equipmentProductivityRepository.getEquipmentProductivity()
        //get calendar key and value
        var colStartDate: OffsetDateTime = OffsetDateTime.now()
        var colEndDate: OffsetDateTime = OffsetDateTime.now()
        if (request != null) {
            when(request.filterType) {
                OrderFilterType.DATE -> {
                    if (request.startDate == null || request.endDate == null) throw BusinessException("")
                    colStartDate = DateTimeHelper.toTimeZone7(request.startDate) ?: OffsetDateTime.now()
                    colEndDate = DateTimeHelper.toTimeZone7(request.endDate) ?: OffsetDateTime.now()
                }
                OrderFilterType.ORDER -> {
                    val plan = request.orderCode?.let { planRepository.getPlanByOrderCode(it) } ?: throw BusinessException("")
                    colStartDate = DateTimeHelper.toTimeZone7(plan.startDate) ?: OffsetDateTime.now()
                    colEndDate = DateTimeHelper.toTimeZone7(plan.endDate) ?: OffsetDateTime.now()
                }
                else -> throw BusinessException("")
            }
        }
        result.columns = DateTimeHelper.toCalendarColumn(colStartDate, colEndDate)


        return result
    }


    fun getPlanDetail(request: PlanDetailRequest): ProductPlanDetailResponse {
        val response = ProductPlanDetailResponse()

        if (request.planProductId.isEmpty()) throw BusinessException("")
        var colStartDate: OffsetDateTime
        var colEndDate: OffsetDateTime

        when (request.filterType) {
            OrderFilterType.DATE -> {
                if (request.startDate == null || request.endDate == null) throw BusinessException("")
                colStartDate = request.startDate ?: OffsetDateTime.now()
                colEndDate = request.endDate ?: OffsetDateTime.now()
            }
            OrderFilterType.ORDER -> {
                val plan = planRepository.getPlanByOrderCode(request.orderCode) ?: throw BusinessException("")
                colStartDate = plan.startDate ?: OffsetDateTime.now()
                colEndDate = plan.endDate ?: OffsetDateTime.now()
            }
            else -> throw BusinessException("")
        }

        response.columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(colStartDate)!!, DateTimeHelper.toTimeZone7(colEndDate)!!)

        val planProduct = planProductRep.getById(request.planProductId) ?: throw BusinessException("")

        val planProcesses = planProcessRep.getListPlanProcess(request.planProductId)
        val parentPlanProcess = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }
        val childrenPlanProcess = planProcesses.filter { x -> x.parentId != null }

        val planProcessIds = parentPlanProcess.mapNotNull { x -> x.id }
        val planDetails = planDetailRep.getPlanDetail(planProcessIds)

        val workResults = workResultRep.getForPlan(colStartDate, colEndDate, listOf(planProduct.productName ?: ""))

        response.data = parentPlanProcess.map { x ->
            val planDetailByProcess = planDetails.filter { m -> m.planProcessId == x.id }
            val workResultData = workResults.filter { m -> m.processCode == x.processCode && m.layerCode == x.layerCode }
            val productPlan = ProductPlanDetailModel(
                layerCode = x.layerCode,
                processCode = x.processCode,
                processName = x.processName,
                completionRate = x.completionRate,
                processConvertCode = x.processConvertCode,
                inventory = x.inventory
            )
            productPlan.processChildren = childrenPlanProcess.filter { m -> m.parentId == x.id }.map { m ->
                ProcessChildrenModel(
                    layerCode = m.layerCode,
                    processCode = m.processCode,
                    processName = m.processName,
                    inventory = m.inventory
                )
            }
            productPlan.sumInventory = (productPlan.processChildren?.sumOf { m -> m.inventory ?: 0 } ?: 0) + (productPlan.inventory ?: 0)

            val planData = mutableListOf<PlanDataByProcessModel>()
            for (title in PlanTitle.DATA) {
                when (title.key) {
                    PlanTitle.PLAN_KEY -> {
                        planData.add(
                            PlanDataByProcessModel(
                                title = title.value,
                                quantityByCalendars = planDetailByProcess.filter { t -> t.title == title.key }.map { t ->
                                    KeyValueResponse(
                                        DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                                        if (x.unit == ProcessUnit.BLOCK) t.blockQuantity?.toString() else t.sheetQuantity?.toString()
                                    )
                                }
                            )
                        )
                    }
                }
            }

            productPlan.planData = PlanTitle.DATA.map { m ->
                PlanDataByProcessModel(
                    title = m.value,
                    quantityByCalendars = planDetailByProcess.filter { t -> t.title == m.key }.map { t ->
                        KeyValueResponse(
                            DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                            t.blockQuantity?.toString()
                        )
                    }
                )
            }
            productPlan
        }
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