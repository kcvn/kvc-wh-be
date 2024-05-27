package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.EquipmentProductivityModel
import com.kcvn.spm.app.plan.payload.model.PlanDataByProcessModel
import com.kcvn.spm.app.plan.payload.model.PlanExportExcelModel
import com.kcvn.spm.app.plan.payload.model.PlanSummaryDetailModel
import com.kcvn.spm.app.plan.payload.model.PlanSummaryModel
import com.kcvn.spm.app.plan.payload.model.ProcessChildrenModel
import com.kcvn.spm.app.plan.payload.model.ProcessDetailListModel
import com.kcvn.spm.app.plan.payload.model.ProcessDetailModel
import com.kcvn.spm.app.plan.payload.model.ProductPlanDetailModel
import com.kcvn.spm.app.plan.payload.model.ProductPlanModel
import com.kcvn.spm.app.plan.payload.request.PlanDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.PagingEquipmentProdResponse
import com.kcvn.spm.app.plan.payload.response.PlanSummaryResponse
import com.kcvn.spm.app.plan.payload.response.ProductPlanDetailResponse
import com.kcvn.spm.common.constants.Color
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.EquipmentType
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.Frame1
import com.kcvn.spm.common.constants.KeyAppSetting
import com.kcvn.spm.common.constants.MasterDataType
import com.kcvn.spm.common.constants.Mold
import com.kcvn.spm.common.constants.PlanActiveTab
import com.kcvn.spm.common.constants.PlanStyleKey
import com.kcvn.spm.common.constants.PlanTitle
import com.kcvn.spm.common.constants.ProcessCode
import com.kcvn.spm.common.constants.ProcessConvertCode
import com.kcvn.spm.common.constants.ProcessPlan
import com.kcvn.spm.common.constants.ProcessStatisticCode
import com.kcvn.spm.common.constants.ProcessUnit
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.NumberHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.CalendarResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.CellStyleModel
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.EquipmentProductivity
import com.kcvn.spm.model.tables.pojos.PlanProduct
import com.kcvn.spm.model.tables.pojos.ProcessGroup
import com.kcvn.spm.repository.AppSettingRepository
import com.kcvn.spm.repository.CommonCategoryRepository
import com.kcvn.spm.repository.EquipmentProductivityRepository
import com.kcvn.spm.repository.HolidaysCalenderRepository
import com.kcvn.spm.repository.PlanColorConfigRepository
import com.kcvn.spm.repository.PlanDetailRepository
import com.kcvn.spm.repository.PlanProcessRepository
import com.kcvn.spm.repository.PlanProductRepository
import com.kcvn.spm.repository.PlanRepository
import com.kcvn.spm.repository.ProcessGroupRepository
import com.kcvn.spm.repository.ProcessMasterRepository
import com.kcvn.spm.repository.WorkResultRepository
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
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
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import kotlin.math.ceil

@Service
@Transactional
class PlanService(
    private val planRep: PlanRepository,
    private val planProductRep: PlanProductRepository,
    private val planProcessRep: PlanProcessRepository,
    private val planDetailRep: PlanDetailRepository,
    private val workResultRep: WorkResultRepository,
    private val holidaysCalenderRep: HolidaysCalenderRepository,
    private val processGroupRep: ProcessGroupRepository,
    private val appSettingRep: AppSettingRepository,
    private val commonCategoryRep: CommonCategoryRepository,
    private val equipmentProductivityRep: EquipmentProductivityRepository,
    private val processMasterRep: ProcessMasterRepository,
    private val planHistoryService: PlanHistoryService,
    private val planColorConfigRep: PlanColorConfigRepository
) {
    //region PLAN
    fun getListPlan(request: PlanSearchRequest, pageable: Pageable): BasePagingResponse<ProductPlanModel> {
        val data = planProductRep.getListPlanProduct(request, pageable)
        val plans = data.first.map { x ->
            ProductPlanModel(
                id = x.id,
                productName = x.productName,
                frame_1 = x.frame_1,
                mold = x.mold,
                pcsSh = x.pcsSh,
                blockSh = x.blockSh
            )
        }
        return BasePagingResponse(plans, data.second)
    }

    fun getPlanDetail(request: PlanDetailRequest): ProductPlanDetailResponse {
        val response = ProductPlanDetailResponse()

        if (request.productName.isEmpty()) throw BusinessException(CommonUtils.getMessage("plan.invalidParam"))
        if (request.startDate == null || request.endDate == null) throw BusinessException(CommonUtils.getMessage("plan.invalidTime"))

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        response.columns = DateTimeHelper.toCalendarColumn(
            DateTimeHelper.toTimeZone7(request.startDate)!!,
            DateTimeHelper.toTimeZone7(request.endDate)!!,
            holidayCalenders
        )

        val planProducts = planProductRep.getForPlan(request)
        if (planProducts.isEmpty()) throw BusinessException(CommonUtils.getMessage("data.notExist"))
        val planProductIds = planProducts.mapNotNull { it.id }
        val productNames = planProducts.mapNotNull { it.productName }.distinct()

        val data = mutableListOf<ProductPlanDetailModel>()

        val planProcesses = planProcessRep.getListPlanProcess(planProductIds, request.draftWorkPlan ?: false, request.processGroups)
        var parentPlanProcesses = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }
        val childrenPlanProcesses = planProcesses.filter { x -> x.parentId != null }

        var planProcessIds = parentPlanProcesses.mapNotNull { x -> x.id }
        val planDetails = planDetailRep.getPlanDetail(
            planProcessIds, request.startDate!!, request.endDate!!,
            request.inventoryWorkPlan ?: false, request.draftWorkPlan ?: false
        )

        planProcessIds = planDetails.mapNotNull { x -> x.planProcessId }
        parentPlanProcesses = parentPlanProcesses.filter { x -> planProcessIds.any { m -> m == x.id } }

        val workResults = workResultRep.getForPlan(request.startDate!!, request.endDate!!, productNames)

        val colorConfigs = planColorConfigRep.getAll()
        val lstOrderDate = planDetails.mapNotNull { it.orderDate }.sortedBy { it }.distinct()
        var indexColor = 0
        val mappingColors = mutableListOf<Pair<OffsetDateTime, String>>()
        for (item in lstOrderDate) {
            if (indexColor >= colorConfigs.size - 1) indexColor = 0
            mappingColors.add(Pair(item, colorConfigs[indexColor].color ?: "#FFFFFF"))
            indexColor++
        }

        for (iPlanProduct in planProducts) {
            val parentPlanProcess = parentPlanProcesses.filter { it.planProductId == iPlanProduct.id }
            val childrenPlanProcess = childrenPlanProcesses.filter { it.planProductId == iPlanProduct.id }

            val results = parentPlanProcess.map { x ->
                val productPlan = ProductPlanDetailModel(
                    frame_1 = iPlanProduct.frame_1,
                    mold = iPlanProduct.mold,
                    layerCode = x.layerCode,
                    processCode = x.processCode,
                    processName = x.processName,
                    processNameJp = x.processNameJp,
                    completionRate = x.completionRate,
                    processConvertCode = x.processConvertCode,
                    processSequence = x.processSequence,
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

                val planDetailByProcess = planDetails.filter { m -> m.planProcessId == x.id }
                val planDetail = planDetailByProcess.filter { m -> m.title == PlanTitle.PLAN_KEY }.groupBy { it.planDate }.map { m ->
                    val firstValue = m.value.first()
                    KeyValueResponse(
                        key = DateTimeHelper.toString(DateTimeHelper.toTimeZone7(m.key)!!, DateTimeFormat.yyyyMMdd),
                        value = if (x.unit == ProcessUnit.BLOCK) m.value.sumOf { it.blockQuantity ?: 0 }.toString() else m.value.sumOf { it.sheetQuantity ?: 0 }.toString(),
                        color = if (m.value.any { !it.orderDate!!.isEqual(firstValue.orderDate) }) "#FFFFFF" else mappingColors.find { it.first == firstValue.orderDate }?.second ?: "#FFFFFF"
                    )
                }.sortedBy { m -> m.key }

                val workResultData = workResults.filter { m -> m.processCode == x.processCode && m.layerCode == x.layerCode }
                    .groupBy { m -> Triple(m.processCode, m.layerCode, DateTimeHelper.toString(m.summaryResultDate!!, DateTimeFormat.yyyyMMdd)) }.map { m ->
                        KeyValueResponse(
                            m.key.third,
                            if (x.unit == ProcessUnit.BLOCK) m.value.sumOf { t -> t.goodTapeQuantity ?: 0 }.toString() else m.value.sumOf { t -> t.goodSheetQuantity ?: 0 }.toString()
                        )
                    }.sortedBy { m -> m.key }

                val planData = mutableListOf<PlanDataByProcessModel>()
                planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN, titleKey = PlanTitle.PLAN_KEY, quantityByCalendars = planDetail, sort = 1))
                planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL, titleKey = PlanTitle.ACTUAL_KEY, quantityByCalendars = workResultData, sort = 3))

                productPlan.planData = planData
                productPlan
            }.sortedWith(compareBy<ProductPlanDetailModel> { x -> x.layerCode?.toInt() }.thenBy { x -> x.processSequence })

            data.addAll(results)
        }

        response.data = data.groupBy { Pair(it.processCode, it.layerCode) }.map { item ->
            val process = item.value.first()
            val rs = ProductPlanDetailModel(
                frame_1 = process.frame_1,
                mold = process.mold,
                layerCode = item.key.second,
                processCode = item.key.first,
                processName = process.processName,
                processNameJp = process.processNameJp,
                completionRate = process.completionRate,
                processConvertCode = process.processConvertCode,
                processStatisticCode = process.processStatisticCode,
                processGroup = process.processGroup,
                processSequence = process.processSequence,
                inventory = item.value.sumOf { it.inventory ?: 0 },
                sumInventory = item.value.sumOf { it.sumInventory ?: 0 },
                processChildren = process.processChildren,
                unit = process.unit,
            )
            val planData = mutableListOf<PlanDataByProcessModel>()
            planData.addAll(
                item.value.asSequence().mapNotNull { it.planData }.flatten().groupBy { it.titleKey }.map { x ->
                    PlanDataByProcessModel(
                        title = x.value.first().title,
                        titleKey = x.key,
                        quantityByCalendars = x.value.mapNotNull { it.quantityByCalendars }.flatten().groupBy { it.key }.map { m ->
                            val colors = m.value.mapNotNull { it.color }.distinct()
                            KeyValueResponse(
                                key = m.key,
                                value = m.value.sumOf { it.value?.toInt() ?: 0 }.toString(),
                                color = if (colors.size == 1) colors.first() else "#FFFFFF"
                            )
                        },
                        sort = x.value.first().sort,
                        total = x.value.mapNotNull { it.quantityByCalendars }.flatten().sumOf { it.value?.toInt() ?: 0 }.toString()
                    )
                }
            )
            planData.add(
                PlanDataByProcessModel(
                    title = PlanTitle.PLAN_ACCUMULATION,
                    titleKey = PlanTitle.PLAN_ACCUMULATION_KEY,
                    quantityByCalendars = calculateAccumulation(planData.first { it.titleKey == PlanTitle.PLAN_KEY }.quantityByCalendars!!),
                    sort = 2
                )
            )
            planData.add(
                PlanDataByProcessModel(
                    title = PlanTitle.ACTUAL_ACCUMULATION,
                    titleKey = PlanTitle.ACTUAL_ACCUMULATION_KEY,
                    quantityByCalendars = calculateAccumulation(planData.first { it.titleKey == PlanTitle.ACTUAL_KEY }.quantityByCalendars!!),
                    sort = 4
                )
            )
            planData.add(
                PlanDataByProcessModel(
                    title = PlanTitle.DIFFERENCE,
                    titleKey = PlanTitle.DIFFERENCE_KEY,
                    quantityByCalendars = calculateDifference(
                        planData.first { it.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY }.quantityByCalendars!!,
                        planData.first { it.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY }.quantityByCalendars!!,
                        response.columns!!
                    ),
                    sort = 5
                )
            )
            rs.planData = planData.sortedBy { it.sort }
            rs
        }
        return response
    }

    private fun getDataExportExcel(
        planProducts: List<PlanProduct>,
        colStartDate: OffsetDateTime,
        colEndDate: OffsetDateTime,
        columns: List<CalendarResponse>,
        hasInventory: Boolean,
        isDraft: Boolean,
        processGroups: String?,
        isSummary: Boolean = false
    ): List<PlanExportExcelModel> {
        val planProductIds = planProducts.mapNotNull { x -> x.id }
        val productNames = planProducts.mapNotNull { x -> x.productName }.distinct()

        val planProcesses = planProcessRep.getListPlanProcess(planProductIds, isDraft, processGroups)
        var parentPlanProcesses = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }.sortedBy { x -> x.planProductId }
        val childrenPlanProcesses = planProcesses.filter { x -> x.parentId != null }

        var planProcessIds = parentPlanProcesses.mapNotNull { x -> x.id }
        val planDetails = planDetailRep.getPlanDetail(planProcessIds, colStartDate, colEndDate, hasInventory, isDraft)

        planProcessIds = planDetails.mapNotNull { x -> x.planProcessId }
        parentPlanProcesses = parentPlanProcesses.filter { x -> planProcessIds.any { m -> m == x.id } }

        val workResults = workResultRep.getForPlan(colStartDate, colEndDate, productNames)

        val data = mutableListOf<PlanExportExcelModel>()

        for (planProduct in planProducts) {
            val parentPlanProcess = parentPlanProcesses.filter { it.planProductId == planProduct.id }
            val childrenPlanProcess = childrenPlanProcesses.filter { it.planProductId == planProduct.id }

            val productExport = PlanExportExcelModel(
                id = planProduct.id,
                productName = planProduct.productName,
                frame_1 = planProduct.frame_1,
                mold = planProduct.mold,
                pcsSh = planProduct.pcsSh,
                blockSh = planProduct.blockSh
            )
            productExport.productPlanDetails = parentPlanProcess.filter { x -> x.planProductId == planProduct.id }.map { x ->
                val productPlan = ProductPlanDetailModel(
                    frame_1 = planProduct.frame_1,
                    mold = planProduct.mold,
                    layerCode = x.layerCode,
                    processCode = x.processCode,
                    processName = x.processName,
                    processNameJp = x.processNameJp,
                    completionRate = x.completionRate,
                    processConvertCode = x.processConvertCode,
                    processStatisticCode = x.processStatisticCode,
                    processGroup = x.processGroup,
                    processSequence = x.processSequence,
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

                val planDetailByProcess = planDetails.filter { m -> m.planProcessId == x.id }
                val planDetail = planDetailByProcess.filter { m -> m.title == PlanTitle.PLAN_KEY }.groupBy { it.planDate }.map { m ->
                    KeyValueResponse(
                        DateTimeHelper.toString(DateTimeHelper.toTimeZone7(m.key)!!, DateTimeFormat.yyyyMMdd),
                        if (x.unit == ProcessUnit.BLOCK) m.value.sumOf { it.blockQuantity ?: 0 }.toString() else m.value.sumOf { it.sheetQuantity ?: 0 }.toString()
                    )
                }.sortedBy { m -> m.key }

                val workResultData = workResults.filter { m -> m.itemName == planProduct.productName && m.processCode == x.processCode && m.layerCode == x.layerCode }
                    .groupBy { m -> Triple(m.processCode, m.layerCode, DateTimeHelper.toString(m.summaryResultDate!!, DateTimeFormat.yyyyMMdd)) }.map { m ->
                        KeyValueResponse(
                            m.key.third,
                            if (x.unit == ProcessUnit.BLOCK) m.value.sumOf { t -> t.goodTapeQuantity ?: 0 }.toString() else m.value.sumOf { t -> t.goodSheetQuantity ?: 0 }.toString()
                        )
                    }.sortedBy { m -> m.key }

                val planData = mutableListOf<PlanDataByProcessModel>()
                planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN, titleKey = PlanTitle.PLAN_KEY, quantityByCalendars = planDetail, sort = 1))
                planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL, titleKey = PlanTitle.ACTUAL_KEY, quantityByCalendars = workResultData, sort = 3))

                productPlan.planData = planData
                productPlan
            }.sortedWith(compareBy<ProductPlanDetailModel> { x -> x.layerCode?.toInt() }.thenBy { x -> x.processSequence })
            data.add(productExport)
        }

        val response = data.groupBy { it.productName }.map { prod ->
            val product = prod.value.first()
            val res = PlanExportExcelModel(
                productName = product.productName,
                frame_1 = product.frame_1,
                mold = product.mold,
                pcsSh = product.pcsSh,
                blockSh = product.blockSh,
                productPlanDetails = prod.value.mapNotNull { it.productPlanDetails }.flatten().groupBy { Pair(it.processCode, it.layerCode) }.map { item ->
                    val process = item.value.first()
                    val rs = ProductPlanDetailModel(
                        frame_1 = process.frame_1,
                        mold = process.mold,
                        layerCode = item.key.second,
                        processCode = item.key.first,
                        processName = process.processName,
                        processNameJp = process.processNameJp,
                        completionRate = process.completionRate,
                        processConvertCode = process.processConvertCode,
                        processStatisticCode = process.processStatisticCode,
                        processGroup = process.processGroup,
                        processSequence = process.processSequence,
                        inventory = item.value.sumOf { it.inventory ?: 0 },
                        sumInventory = item.value.sumOf { it.sumInventory ?: 0 },
                        processChildren = process.processChildren,
                        unit = process.unit,
                    )
                    val planData = mutableListOf<PlanDataByProcessModel>()
                    planData.addAll(
                        item.value.asSequence().mapNotNull { it.planData }.flatten().groupBy { it.titleKey }.map { x ->
                            PlanDataByProcessModel(
                                title = x.value.first().title,
                                titleKey = x.key,
                                quantityByCalendars = x.value.mapNotNull { it.quantityByCalendars }.flatten().groupBy { it.key }.map { m ->
                                    KeyValueResponse(
                                        m.key,
                                        m.value.sumOf { it.value?.toInt() ?: 0 }.toString(),
                                    )
                                },
                                sort = x.value.first().sort,
                                total = x.value.mapNotNull { it.quantityByCalendars }.flatten().sumOf { it.value?.toInt() ?: 0 }.toString()
                            )
                        }
                    )
                    if (!isSummary) {
                        planData.add(
                            PlanDataByProcessModel(
                                title = PlanTitle.PLAN_ACCUMULATION,
                                titleKey = PlanTitle.PLAN_ACCUMULATION_KEY,
                                quantityByCalendars = calculateAccumulation(planData.first { it.titleKey == PlanTitle.PLAN_KEY }.quantityByCalendars!!),
                                sort = 2
                            )
                        )
                        planData.add(
                            PlanDataByProcessModel(
                                title = PlanTitle.ACTUAL_ACCUMULATION,
                                titleKey = PlanTitle.ACTUAL_ACCUMULATION_KEY,
                                quantityByCalendars = calculateAccumulation(planData.first { it.titleKey == PlanTitle.ACTUAL_KEY }.quantityByCalendars!!),
                                sort = 4
                            )
                        )
                        planData.add(
                            PlanDataByProcessModel(
                                title = PlanTitle.DIFFERENCE,
                                titleKey = PlanTitle.DIFFERENCE_KEY,
                                quantityByCalendars = calculateDifference(
                                    planData.first { it.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY }.quantityByCalendars!!,
                                    planData.first { it.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY }.quantityByCalendars!!,
                                    columns
                                ),
                                sort = 5
                            )
                        )
                    }
                    rs.planData = planData.sortedBy { it.sort }
                    rs
                }
            )
            res
        }
        return response
    }

    private fun calculateAccumulation(data: List<KeyValueResponse>, firstValue: Int? = null): List<KeyValueResponse> {
        var value = firstValue ?: 0
        val response = mutableListOf<KeyValueResponse>()
        for (item in data.sortedBy { it.key }) {
            value += (item.value?.toInt() ?: 0)
            response.add(KeyValueResponse(item.key, value.toString()))
        }
        return response
    }

    private fun calculateDifference(sourceData: List<KeyValueResponse>, compareData: List<KeyValueResponse>, columns: List<CalendarResponse>): List<KeyValueResponse> {
        val response = mutableListOf<KeyValueResponse>()
        for (col in columns) {
            val sourceValue = sourceData.find { x -> x.key == col.key }
            val compareValue = compareData.find { x -> x.key == col.key }

            if (sourceValue != null || compareValue != null) {
                val diffValue = (compareValue?.value?.toInt() ?: 0) - (sourceValue?.value?.toInt() ?: 0)
                response.add(KeyValueResponse(col.key, diffValue.toString()))
            }
        }
        return response
    }

    //endregion

    //region PLAN SUMMARY

    private fun generateModelPlanSummaryData(
        type: String,
        process: ProcessGroup?,
        processConvertCode: String,
        columns: List<CalendarResponse>?,
        values: List<ProductPlanDetailModel>,
        isExport: Boolean = false
    ): PlanSummaryModel {
        val summary = PlanSummaryModel(
            processName = process?.description,
            processNameJp = process?.descriptionJp,
            processConvertCode = processConvertCode,
            processSequence = process?.sortOrder?.toInt(),
        )
        val planSummaryData = values.asSequence().mapNotNull { m -> m.planData }.flatten()
            .groupBy { m -> Pair(m.titleKey, m.title) }
            .map { m ->
                val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first, sort = m.value.first().sort)
                data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                data.total = data.quantityByCalendars?.sumOf { it.value?.toInt() ?: 0 }.toString()
                data
            }.toMutableList()
        if (!isExport) {
            planSummaryData.add(
                PlanDataByProcessModel(
                    title = PlanTitle.PLAN_ACCUMULATION,
                    titleKey = PlanTitle.PLAN_ACCUMULATION_KEY,
                    quantityByCalendars = calculateAccumulation(planSummaryData.first { it.titleKey == PlanTitle.PLAN_KEY }.quantityByCalendars!!),
                    sort = 2
                )
            )
            planSummaryData.add(
                PlanDataByProcessModel(
                    title = PlanTitle.ACTUAL_ACCUMULATION,
                    titleKey = PlanTitle.ACTUAL_ACCUMULATION_KEY,
                    quantityByCalendars = calculateAccumulation(planSummaryData.first { it.titleKey == PlanTitle.ACTUAL_KEY }.quantityByCalendars!!),
                    sort = 4
                )
            )
            planSummaryData.add(
                PlanDataByProcessModel(
                    title = PlanTitle.DIFFERENCE,
                    titleKey = PlanTitle.DIFFERENCE_KEY,
                    quantityByCalendars = calculateDifference(
                        planSummaryData.first { it.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY }.quantityByCalendars!!,
                        planSummaryData.first { it.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY }.quantityByCalendars!!,
                        columns!!
                    ),
                    sort = 5
                )
            )
        }
        summary.details = mutableListOf(PlanSummaryDetailModel(
            type = type,
            planSummaryData = planSummaryData.sortedBy { it.sort }
        ))
        return summary
    }

    private fun generateModelPlanSummaryData1(
        type: String,
        process: ProcessGroup?,
        processConvertCode: String,
        columns: List<CalendarResponse>?,
        values: List<PlanSummaryModel>,
        isExport: Boolean = false
    ): PlanSummaryModel {
        val summary = PlanSummaryModel(
            processName = process?.description,
            processNameJp = process?.descriptionJp,
            processConvertCode = processConvertCode,
            processSequence = process?.sortOrder?.toInt(),
        )
        val planSummaryData = values.asSequence().mapNotNull { x -> x.details }.flatten().mapNotNull { x -> x.planSummaryData }.flatten()
            .filter { it.titleKey == PlanTitle.PLAN_KEY || it.titleKey == PlanTitle.ACTUAL_KEY }
            .groupBy { x -> Pair(x.titleKey, x.title) }.map { x ->
                val data = PlanDataByProcessModel(title = x.key.second, titleKey = x.key.first, sort = x.value.first().sort)
                data.quantityByCalendars = x.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                data.total = data.quantityByCalendars?.sumOf { it.value?.toInt() ?: 0 }.toString()
                data
            }.toMutableList()

        if (!isExport) {
            planSummaryData.add(
                PlanDataByProcessModel(
                    title = PlanTitle.PLAN_ACCUMULATION,
                    titleKey = PlanTitle.PLAN_ACCUMULATION_KEY,
                    quantityByCalendars = calculateAccumulation(planSummaryData.first { it.titleKey == PlanTitle.PLAN_KEY }.quantityByCalendars!!),
                    sort = 2
                )
            )
            planSummaryData.add(
                PlanDataByProcessModel(
                    title = PlanTitle.ACTUAL_ACCUMULATION,
                    titleKey = PlanTitle.ACTUAL_ACCUMULATION_KEY,
                    quantityByCalendars = calculateAccumulation(planSummaryData.first { it.titleKey == PlanTitle.ACTUAL_KEY }.quantityByCalendars!!),
                    sort = 4
                )
            )
            planSummaryData.add(
                PlanDataByProcessModel(
                    title = PlanTitle.DIFFERENCE,
                    titleKey = PlanTitle.DIFFERENCE_KEY,
                    quantityByCalendars = calculateDifference(
                        planSummaryData.first { it.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY }.quantityByCalendars!!,
                        planSummaryData.first { it.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY }.quantityByCalendars!!,
                        columns!!
                    ),
                    sort = 5
                )
            )
        }
        summary.details = mutableListOf(PlanSummaryDetailModel(
            type = type,
            planSummaryData = planSummaryData.sortedBy { it.sort }
        ))
        return summary
    }

    private fun generateModelSummaryDetail(
        type: String,
        columns: List<CalendarResponse>?,
        dataGroupBy: Map<String?, List<ProductPlanDetailModel>>,
        allowReturnNull: Boolean = false,
        isExport: Boolean = false
    ): PlanSummaryDetailModel? {
        var data = dataGroupBy.mapNotNull { x ->
            val planSummaryData = x.value.asSequence().mapNotNull { m -> m.planData }.flatten()
                .filter { it.titleKey == PlanTitle.PLAN_KEY || it.titleKey == PlanTitle.ACTUAL_KEY }
                .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                    val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first, sort = m.value.first().sort)
                    data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                        .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                    data.total = data.quantityByCalendars?.sumOf { it.value?.toInt() ?: 0 }.toString()
                    data
                }.toMutableList()

            if (!isExport) {
                planSummaryData.add(
                    PlanDataByProcessModel(
                        title = PlanTitle.PLAN_ACCUMULATION,
                        titleKey = PlanTitle.PLAN_ACCUMULATION_KEY,
                        quantityByCalendars = calculateAccumulation(planSummaryData.first { it.titleKey == PlanTitle.PLAN_KEY }.quantityByCalendars!!),
                        sort = 2
                    )
                )
                planSummaryData.add(
                    PlanDataByProcessModel(
                        title = PlanTitle.ACTUAL_ACCUMULATION,
                        titleKey = PlanTitle.ACTUAL_ACCUMULATION_KEY,
                        quantityByCalendars = calculateAccumulation(planSummaryData.first { it.titleKey == PlanTitle.ACTUAL_KEY }.quantityByCalendars!!),
                        sort = 4
                    )
                )
                planSummaryData.add(
                    PlanDataByProcessModel(
                        title = PlanTitle.DIFFERENCE,
                        titleKey = PlanTitle.DIFFERENCE_KEY,
                        quantityByCalendars = calculateDifference(
                            planSummaryData.first { it.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY }.quantityByCalendars!!,
                            planSummaryData.first { it.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY }.quantityByCalendars!!,
                            columns!!
                        ),
                        sort = 5
                    )
                )
            }

            PlanSummaryDetailModel(
                type = type,
                planSummaryData = planSummaryData.sortedBy { it.sort }
            )
        }.firstOrNull()
        if (data == null && !allowReturnNull) {
            data = PlanSummaryDetailModel(
                type = type,
                planSummaryData = if (isExport) {
                    PlanTitle.DATA_EXPORT_SUMMARY.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                } else {
                    PlanTitle.DATA.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                }
            )
        }
        return data
    }

    fun getPlanSummary(request: PlanSearchRequest, dataExports: MutableList<PlanExportExcelModel> = mutableListOf(), isExport: Boolean = false): PlanSummaryResponse {
        val response = PlanSummaryResponse()
        if (request.startDate == null || request.endDate == null) throw BusinessException(CommonUtils.getMessage("plan.invalidTime"))

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        response.columns = DateTimeHelper.toCalendarColumn(
            DateTimeHelper.toTimeZone7(request.startDate)!!,
            DateTimeHelper.toTimeZone7(request.endDate)!!,
            holidayCalenders
        )

        val processGroups = processGroupRep.getForPlanSummary()

        val planProducts = planProductRep.getListPlanProduct(request)
        if (dataExports.isEmpty()) {
            dataExports.addAll(getDataExportExcel(
                planProducts, request.startDate!!, request.endDate!!, response.columns!!,
                request.inventoryWorkPlan ?: false, request.draftWorkPlan ?: false, request.processGroups, true
            ))
        }
        val dataExportFlattens = dataExports.asSequence().mapNotNull { x -> x.productPlanDetails }.flatten().filter { x ->
            !x.processConvertCode.isNullOrEmpty()
                && (
                processGroups.any { m -> !m.summaryCode.isNullOrEmpty() && m.summaryCode!!.split("/").contains(x.processConvertCode) }
                    || (x.processConvertCode!!.startsWith(ProcessConvertCode.M) && processGroups.any { m -> m.summaryCode == ProcessStatisticCode.GHEP_LOP_SUM })
                )
        }

        val dataSummary = dataExportFlattens.groupBy { x -> x.processConvertCode }.map { x ->
            val process = processGroups.find { m -> m.processStatisticCode == x.key }
            val summary = generateModelPlanSummaryData("", process, x.key!!, response.columns, x.value, isExport)
            summary
        }.sortedBy { x -> x.processSequence }.toMutableList()

        val dataDucLo = dataSummary.filter { x -> x.processConvertCode == ProcessConvertCode.T || x.processConvertCode == ProcessConvertCode.TH }
        if (dataDucLo.isNotEmpty()) {
            val moldByFrame1s = (appSettingRep.findByKey("${KeyAppSetting.MOLD_BY_FRAME1}_${request.frame_1}")?.value?.split(",")
                ?: Mold.DATA_BY_FRAME1(request.frame_1)).filter { x -> request.mold.isNullOrEmpty() || x == request.mold }
            val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.T }
            val ducLo = generateModelPlanSummaryData1(
                if (moldByFrame1s.size > 1) CommonUtils.getMessage("excel.rowTotal") else moldByFrame1s.first(),
                process, "${ProcessConvertCode.T}/${ProcessConvertCode.TH}", response.columns, dataDucLo, isExport
            )

            if (moldByFrame1s.size > 1) {
                for (mold in moldByFrame1s) {
                    val dataMold = generateModelSummaryDetail(
                        type = mold,
                        columns = response.columns,
                        dataGroupBy = dataExportFlattens.filter { x ->
                            x.mold == mold
                                && (x.processConvertCode == ProcessConvertCode.T || x.processConvertCode == ProcessConvertCode.TH)
                        }.groupBy { x -> x.mold },
                        allowReturnNull = false,
                        isExport = isExport
                    )
                    ducLo.details!!.add(dataMold!!)
                }
            }

            dataSummary.removeAll(dataDucLo)
            dataSummary.add(ducLo)
        }

        val dataInMach = dataSummary.filter { x -> x.processConvertCode == ProcessConvertCode.TAN || x.processConvertCode == ProcessConvertCode.ZEN }
        if (dataInMach.isNotEmpty()) {
            val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.IN_MACH }
            val inMach = generateModelPlanSummaryData1(
                CommonUtils.getMessage("excel.rowTotal"), process,
                "${ProcessConvertCode.TAN}/${ProcessConvertCode.ZEN}", response.columns, dataInMach, isExport
            )

            for (code in listOf(ProcessConvertCode.TAN, ProcessConvertCode.ZEN)) {
                val dataTanZen = generateModelSummaryDetail(
                    type = code,
                    columns = response.columns,
                    dataGroupBy = dataExportFlattens.filter { x -> x.processConvertCode == code }.groupBy { x -> x.processConvertCode },
                    allowReturnNull = false,
                    isExport = isExport
                )
                inMach.details!!.add(dataTanZen!!)
            }

            dataSummary.removeAll(dataInMach)
            dataSummary.add(inMach)
        }

        val dataInLo = dataSummary.filter { x -> x.processConvertCode == ProcessConvertCode.HP_ALL || x.processConvertCode == ProcessConvertCode.HP }
        if (dataInLo.isNotEmpty()) {
            val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.IN_LO }
            val inLo = generateModelPlanSummaryData1("", process, "${ProcessConvertCode.HP_ALL}/${ProcessConvertCode.HP}", response.columns, dataInLo, isExport)
            dataSummary.removeAll(dataInLo)
            dataSummary.add(inLo)
        }

        val dataGhepLop = dataSummary.filter { x -> !x.processConvertCode.isNullOrEmpty() && x.processConvertCode!!.startsWith(ProcessConvertCode.M) }
        if (dataGhepLop.isNotEmpty()) {
            val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.GHEP_LOP_SUM }
            val ghepLop = generateModelPlanSummaryData1(
                CommonUtils.getMessage("excel.rowTotal"), process,
                "${ProcessConvertCode.M_ALL}/M2*3,M3*4,...", response.columns, dataGhepLop, isExport
            )

            val mAll = generateModelSummaryDetail(
                type = ProcessConvertCode.M_ALL,
                columns = response.columns,
                dataGroupBy = dataExportFlattens.filter { x -> x.processConvertCode == ProcessConvertCode.M_ALL }.groupBy { x -> x.processConvertCode },
                allowReturnNull = false,
                isExport = isExport
            )
            ghepLop.details!!.add(mAll!!)

            val mTan = generateModelSummaryDetail(
                type = "M2*3, M3*4...",
                columns = response.columns,
                dataGroupBy = dataExportFlattens.filter { x ->
                    !x.processConvertCode.isNullOrEmpty()
                        && x.processConvertCode != ProcessConvertCode.M_ALL
                        && x.processConvertCode!!.startsWith(ProcessConvertCode.M)
                }.map { x ->
                    x.processConvertCode = ProcessConvertCode.M_ANY
                    x
                }.groupBy { x -> x.processConvertCode },
                allowReturnNull = false,
                isExport = isExport
            )
            ghepLop.details!!.add(mTan!!)

            val mGAN = generateModelSummaryDetail(
                type = "Ghép lớp gia áp nhiệt",
                columns = response.columns,
                dataGroupBy = dataExportFlattens.filter { x ->
                    x.processStatisticCode == ProcessStatisticCode.GHEPLOP_GIAAPNHIET
                }.groupBy { x -> x.processConvertCode },
                allowReturnNull = true,
                isExport = isExport
            )
            if (mGAN != null) {
                ghepLop.details!!.add(mGAN)
            }

            val mGLT = generateModelSummaryDetail(
                type = "Ghép lớp thường",
                columns = response.columns,
                dataGroupBy = dataExportFlattens.filter { x ->
                    !x.processConvertCode.isNullOrEmpty()
                        && x.processStatisticCode != ProcessStatisticCode.GHEPLOP_GIAAPNHIET
                        && x.processConvertCode!!.startsWith(ProcessConvertCode.M)
                }.groupBy { x -> x.processConvertCode },
                allowReturnNull = true,
                isExport = isExport
            )
            if (mGLT != null) {
                ghepLop.details!!.add(mGLT)
            }

            dataSummary.removeAll(dataGhepLop)
            dataSummary.add(ghepLop)
        }

        response.data = dataSummary.sortedBy { x -> x.processSequence }

        return response

    }

    private fun generateExcelColProcessInPlanSummary(
        workbook: Workbook,
        sheet: Sheet,
        rowNumber: Int,
        style: CellStyle,
        data: PlanSummaryModel,
        styleCollections: MutableList<CellStyleModel>,
        activeTab: String = PlanActiveTab.SUM
    ): Int {
        var rowIndex = rowNumber
        var dataRow = ExcelHelper.createRow(sheet, rowIndex)
        val increaseRowIndex = if (activeTab == PlanActiveTab.SUM) 0 else 9
        val value = if (activeTab == PlanActiveTab.SUM) "${data.processName} [${data.processConvertCode}]" else data.processName
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 1 + increaseRowIndex, styleTemplate = style, value = value,
            isBorderLeft = true, isBorderRight = true, isBorderTop = true, isBorderBottom = false,
            isBold = false, isAlignCenter = false
        )
        styleCollections.add(CellStyleModel(1 + increaseRowIndex, dataRow.getCell(1 + increaseRowIndex).cellStyle, PlanStyleKey.PLAN_SUMMARY_FIRST_ROW))

        if (activeTab == PlanActiveTab.SUM) {
            rowIndex++
            dataRow = ExcelHelper.createRow(sheet, rowIndex)
            ExcelHelper.setCellValueCustom(
                workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = data.processNameJp,
                isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false,
                isBold = false, isAlignCenter = false
            )
            styleCollections.add(CellStyleModel(1, dataRow.getCell(1).cellStyle, PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW))

            if (data.details!!.size == 1) {
                ExcelHelper.setCellValueCustom(
                    workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = data.processNameJp,
                    isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = true,
                    isBold = false, isAlignCenter = false
                )
                styleCollections.add(CellStyleModel(1, dataRow.getCell(1).cellStyle, PlanStyleKey.PLAN_SUMMARY_END_ROW))
            }

            rowIndex++
            for (i in 2 until (data.details!!.size * 2)) {
                dataRow = ExcelHelper.createRow(sheet, rowIndex)
                ExcelHelper.setCellValueCustom(
                    workbook = workbook, row = dataRow, colIndex = 1, styleTemplate = style, value = "",
                    isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = (i == ((data.details!!.size * 2) - 1)),
                    isBold = false, isAlignCenter = false
                )
                if (i == ((data.details!!.size * 2) - 1)) {
                    styleCollections.add(CellStyleModel(1, dataRow.getCell(1).cellStyle, PlanStyleKey.PLAN_SUMMARY_END_ROW))
                }
                rowIndex++
            }
            return rowIndex
        } else {
            rowIndex++
            dataRow = ExcelHelper.createRow(sheet, rowIndex)
            ExcelHelper.setCellValueCustom(
                workbook = workbook, row = dataRow, colIndex = 10, styleTemplate = style, value = data.processNameJp,
                isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false,
                isBold = false, isAlignCenter = false
            )
            styleCollections.add(CellStyleModel(10, dataRow.getCell(10).cellStyle, PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW))

            rowIndex++
            dataRow = ExcelHelper.createRow(sheet, rowIndex)
            ExcelHelper.setCellValueCustom(
                workbook = workbook, row = dataRow, colIndex = 10, styleTemplate = style, value = data.processConvertCode,
                isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false,
                isBold = false, isAlignCenter = false
            )

            rowIndex++
            for (i in 3 until (data.details!!.size * 5)) {
                dataRow = ExcelHelper.createRow(sheet, rowIndex)
                ExcelHelper.setCellValueCustom(
                    workbook = workbook, row = dataRow, colIndex = 10, styleTemplate = style, value = "",
                    isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = (i == ((data.details!!.size * 5) - 1)),
                    isBold = false, isAlignCenter = false
                )
                if (i == ((data.details!!.size * 5) - 1)) {
                    styleCollections.add(CellStyleModel(10, dataRow.getCell(10).cellStyle, PlanStyleKey.PLAN_SUMMARY_END_ROW))
                }
                rowIndex++
            }
            return rowIndex
        }
    }

    private fun generateExcelColProcessInPlanSummary(
        sheet: Sheet,
        rowNumber: Int,
        data: PlanSummaryModel,
        styleCollections: MutableList<CellStyleModel>,
        activeTab: String = PlanActiveTab.SUM
    ): Int {
        var rowIndex = rowNumber
        var dataRow = ExcelHelper.createRow(sheet, rowIndex)
        val increaseRowIndex = if (activeTab == PlanActiveTab.SUM) 0 else 9
        val value = if (activeTab == PlanActiveTab.SUM) "${data.processName} [${data.processConvertCode}]" else data.processName
        ExcelHelper.setCellValue(dataRow, 1 + increaseRowIndex, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle, value)

        if (activeTab == PlanActiveTab.SUM) {
            rowIndex++
            dataRow = ExcelHelper.createRow(sheet, rowIndex)

            val style = if (data.details!!.size == 1) {
                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
            } else {
                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
            }

            ExcelHelper.setCellValue(dataRow, 1, style, data.processNameJp)

            rowIndex++
            for (i in 2 until (data.details!!.size * 2)) {
                dataRow = ExcelHelper.createRow(sheet, rowIndex)
                val st = if (i == ((data.details!!.size * 2) - 1)) {
                    styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                } else {
                    styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
                }
                ExcelHelper.setCellValue(dataRow, 1, st, "")
                rowIndex++
            }
            return rowIndex
        } else {
            rowIndex++
            dataRow = ExcelHelper.createRow(sheet, rowIndex)
            ExcelHelper.setCellValue(dataRow, 10, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle, data.processNameJp)

            rowIndex++
            dataRow = ExcelHelper.createRow(sheet, rowIndex)
            ExcelHelper.setCellValue(dataRow, 10, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle, data.processConvertCode)

            rowIndex++
            for (i in 3 until (data.details!!.size * 5)) {
                dataRow = ExcelHelper.createRow(sheet, rowIndex)
                val st = if (i == ((data.details!!.size * 5) - 1)) {
                    styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                } else {
                    styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
                }
                ExcelHelper.setCellValue(dataRow, 10, st, "")
                rowIndex++
            }
            return rowIndex
        }
    }

    //endregion

    //region EQUIPMENT
    fun getEquipmentProductivityPlan(request: PlanSearchRequest): PagingEquipmentProdResponse? {
        val planSummary = getPlanSummaryEquipment(request)
        val planSummaryData = planSummary.data
        val listGroupProcessCode = planSummaryData?.mapNotNull { x -> x.processCode }?.let { processMasterRep.getProcessMasterDataByCode(it) }

        val equipmentMachine = equipmentProductivityRep.getEquipmentProductivity()

        val result = PagingEquipmentProdResponse()
        result.columns = planSummary.columns
        val equipmentProductivityList = mutableListOf<EquipmentProductivityModel>()
        planSummary.data?.let { data ->
            for (planSummaryModel in data) {
                val equipmentProductivityModel = EquipmentProductivityModel(
                    frame1 = planSummaryModel.frame1,
                    processName = planSummaryModel.processName,
                    processNameJp = planSummaryModel.processNameJp,
                    processConvertCode = planSummaryModel.processConvertCode,
                    processCode = planSummaryModel.processCode,
                    unit = planSummaryModel.unit,
                    processDetail = mutableListOf()
                )
                val groupProcessCode = listGroupProcessCode?.filter { x -> x.processCode == planSummaryModel.processCode }?.map { it.groupProcessCode }
                planSummaryModel.details?.let { details ->
                    for (processSummaryDetailModel in details) {
                        //get equipment machine by process code, frame1, type
                        val filteredEquipmentMachine = equipmentMachine.filter {
                            groupProcessCode?.contains(it.grpProcess) == true &&
                                it.frame_1 == planSummaryModel.frame1 &&
                                (it.mold?.contains(processSummaryDetailModel.type ?: "") == true
                                    || it.processName?.contains(processSummaryDetailModel.type ?: "") == true
                                    )
                        }

                        var equipmentMachineModel = filteredEquipmentMachine.ifEmpty {
                            equipmentMachine.filter {
                                groupProcessCode?.contains(it.grpProcess) == true &&
                                    it.frame_1 == planSummaryModel.frame1
                            }
                        }

                        if (planSummaryModel.processConvertCode == ProcessPlan.PROCESS_DUC_LO_CVC) {
                            equipmentMachineModel = equipmentMachine.filter { x ->
                                x.grpProcess.equals(ProcessCode.T_TH) && x.frame_1 == planSummaryModel.frame1
                                    && x.mold?.contains(processSummaryDetailModel.type ?: "") == true
                            }
                        }
                        val processDetailModel = ProcessDetailModel(
                            name = processSummaryDetailModel.type,
                            totalProcess = null,
                            processDetailList = mutableListOf()
                        )
                        processSummaryDetailModel.planSummaryData?.let { planData ->
                            for (planDataByProcessModel in planData) {
                                val quantityMachine = equipmentMachineModel.firstOrNull()?.quantityMachine?.toDouble()

                                //process
                                val processDetailListModel = ProcessDetailListModel(
                                    type = planDataByProcessModel.title ?: "",
                                    typeKey = EquipmentType.PROCESS,
                                    quantityByCalendars = planDataByProcessModel.quantityByCalendars?.toMutableList() ?: mutableListOf()
                                )
                                processDetailModel.processDetailList.add(processDetailListModel)
                                //calculate total
                                processDetailModel.totalProcess = processDetailListModel.quantityByCalendars
                                    .mapNotNull { it.value?.toDoubleOrNull() }
                                    .sum()
                                    .toInt()

                                //machine
                                val machineDetailListModel = getMachineDetail(result.columns, equipmentMachineModel, equipmentProductivityModel)
                                processDetailModel.processDetailList.add(machineDetailListModel)

                                //average plan
                                val averageDetailListModel = getMachineDetail(result.columns, equipmentMachineModel, equipmentProductivityModel, false)
                                processDetailModel.processDetailList.add(averageDetailListModel)
                                //machineRate
                                val machineQuantityDetailListModel = calculateMachineDetail(quantityMachine, processDetailListModel.quantityByCalendars, averageDetailListModel.quantityByCalendars)
                                processDetailModel.processDetailList.add(machineQuantityDetailListModel)

                            }
                        }
                        equipmentProductivityModel.processDetail?.add(processDetailModel)
                    }

                }

                equipmentProductivityList.add(equipmentProductivityModel)
            }
        }

        result.data = equipmentProductivityList
        return result

    }

    fun getMachineDetail(
        columns: List<CalendarResponse>?,
        equipmentMachineModel: List<EquipmentProductivity>,
        equipmentProductivityModel: EquipmentProductivityModel,
        isGetCapMachine: Boolean = true
    ): ProcessDetailListModel {
        var capMachineValue: BigDecimal?
        val machineDetailListModel = ProcessDetailListModel(
            type = if (isGetCapMachine) ProcessPlan.MACHINE else ProcessPlan.AVERAGE_PLAN,
            typeKey = if (isGetCapMachine) EquipmentType.CAP_MACHINE else EquipmentType.AVERAGE,
            quantityByCalendars = columns?.map { column ->
                val convertDate = DateTimeHelper.convertStringToOffSetDateTime(column.key, DateTimeFormat.yyyyMMdd).toLocalDate()
                val convertEquipment = equipmentMachineModel.firstOrNull { equipment ->
                    val productionStartDate = DateTimeHelper.toTimeZone7(equipment.productionStartDate)?.toLocalDate()
                    val productionEndDateLocal = DateTimeHelper.toTimeZone7(equipment.productionEndDate)?.toLocalDate()
                    productionStartDate != null && productionStartDate <= convertDate && (productionEndDateLocal == null || productionEndDateLocal >= convertDate)
                }
                if (isGetCapMachine) {
                    capMachineValue = when {
                        convertEquipment != null -> {
                            when (equipmentProductivityModel.unit) {
                                ProcessUnit.SHEET -> convertEquipment.capSheet
                                ProcessUnit.SET -> convertEquipment.capSet
                                else -> convertEquipment.capBlock
                            }
                        }

                        else -> null
                    }
                } else {
                    capMachineValue = when {
                        convertEquipment != null -> {
                            when (equipmentProductivityModel.unit) {
                                ProcessUnit.SHEET -> convertEquipment.sltbSheet
                                ProcessUnit.SET -> convertEquipment.sltbSet
                                else -> convertEquipment.sltbBlock
                            }
                        }

                        else -> null
                    }
                }
                KeyValueResponse(
                    key = column.key,
                    value = capMachineValue?.toInt()?.toString() ?: ""
                )
            }?.toMutableList() ?: mutableListOf()
        )
        return machineDetailListModel
    }

    fun calculateMachineDetail(
        quantityMachine: Double?,
        processDetails: List<KeyValueResponse>?,
        averageProductionDetails: List<KeyValueResponse>?
    ): ProcessDetailListModel {
        val machineNumberDetailListModel = ProcessDetailListModel(
            type = ProcessPlan.QUANTITY_MACHINE,
            typeKey = EquipmentType.MACHINE_RATE,
            quantityByCalendars = processDetails?.map { column ->
                val value = column.value?.toDoubleOrNull() ?: 0.0
                val averageProductionValueDouble = averageProductionDetails?.firstOrNull { x -> x.key == column.key }?.value?.toDoubleOrNull() ?: 0.0
                val newValue = if (averageProductionValueDouble != 0.0) {
                    NumberHelper.formatDoubleValue(value / averageProductionValueDouble)
                } else {
                    "0"
                }
                var rate = 0.0
                if (averageProductionValueDouble != 0.0) {
                    if (quantityMachine != null) {
                        rate = newValue.toDouble() / quantityMachine.toDouble() * 100
                    }
                }
                val color = when {
                    rate > 100 -> Color.ORANGE
                    rate > 90 -> Color.YELLOW
                    else -> Color.WHITE
                }
                val valueRate = when {
                    quantityMachine == null || quantityMachine.toInt() == 0 -> ""
                    else -> newValue + "/" + (quantityMachine.toInt()).toString() + "\n" + (ceil(rate).toInt()).toString()
                }
                KeyValueResponse(
                    key = column.key,
                    value = valueRate,
                    sort = color.toBigDecimal()
                )
            }?.toMutableList() ?: mutableListOf()
        )

        return machineNumberDetailListModel
    }


    private fun getPlanSummaryEquipment(request: PlanSearchRequest): PlanSummaryResponse {
        val response = PlanSummaryResponse()
        if (request.startDate == null || request.endDate == null) throw BusinessException(CommonUtils.getMessage("plan.invalidTime"))

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        val processGroups = processGroupRep.getForPlanEquipmentProductivity()
        response.columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(request.startDate)!!, DateTimeHelper.toTimeZone7(request.endDate)!!, holidayCalenders)

        val planProducts = planProductRep.getListPlanProduct(request)
        val dataExports = getDataExportExcelEquipment(planProducts, request)
        val dataExportFlattens = dataExports.asSequence().mapNotNull { x -> x.productPlanDetails }.flatten().filter { x ->
            !x.processConvertCode.isNullOrEmpty()
                && (
                processGroups.any { m -> !m.summaryCode.isNullOrEmpty() && m.summaryCode!!.split("/").contains(x.processConvertCode) }
                    || (x.processConvertCode!!.startsWith(ProcessConvertCode.M) && processGroups.any { m -> m.summaryCode == ProcessStatisticCode.GHEP_LOP_SUM })
                )
        }

        val types = listOf(MasterDataType.KHUNG_1)
        val dataFrame = commonCategoryRep.getByType(types)
        val listFrame1 = dataFrame.map { x -> x.value }

        val dataSummary = dataExportFlattens.groupBy { x -> Pair(x.processConvertCode, x.frame_1) }.map { x ->
            val process = processGroups.find { m -> m.processStatisticCode == x.key.first }
            val processMaster = x.value.first()

            val summary = PlanSummaryModel(
                processName = process?.description,
                processNameJp = process?.descriptionJp,
                processConvertCode = x.key.first,
                processSequence = process?.sortOrder?.toInt(),
                frame1 = processMaster.frame_1,
                processCode = processMaster.processCode,
                unit = processMaster.unit,
                details = mutableListOf(
                    PlanSummaryDetailModel(
                        type = "",
                        planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                            .groupBy { m -> Pair(m.titleKey, m.title) }
                            .map { m ->
                                val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }
                    )
                )
            )
            summary
        }.toMutableList()

        for (frame1 in listFrame1) {
            val dataDucLo = dataSummary.filter { x -> x.frame1 == frame1 && (x.processConvertCode == ProcessConvertCode.T || x.processConvertCode == ProcessConvertCode.TH) }
            if (dataDucLo.isNotEmpty()) {
                var moldByFrame1s = (appSettingRep.findByKey("${KeyAppSetting.MOLD_BY_FRAME1}_${frame1}")?.value?.split(",")
                    ?: Mold.DATA_BY_FRAME1(frame1)).filter { x -> request.mold.isNullOrEmpty() || x == request.mold }
                val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.T }
                val listMoldRequest: MutableList<String> = mutableListOf()
                if (!request.mold.isNullOrEmpty()) {
                    listMoldRequest.add(request.mold!!)
                    moldByFrame1s = listMoldRequest
                }
                val ducLo = PlanSummaryModel(
                    processName = process?.description,
                    processNameJp = process?.descriptionJp,
                    processConvertCode = "${ProcessConvertCode.T}/${ProcessConvertCode.TH}",
                    processSequence = process?.sortOrder?.toInt(),
                    details = mutableListOf(),
                    frame1 = dataDucLo.first().frame1,
                    processCode = dataDucLo.first().processCode,
                    unit = dataDucLo.first().unit
                )
                if (moldByFrame1s.isNotEmpty()) {
                    for (mold in moldByFrame1s) {
                        var dataMold = dataExportFlattens.filter { x ->
                            x.mold == mold
                                && (x.processConvertCode == ProcessConvertCode.T || x.processConvertCode == ProcessConvertCode.TH)
                        }.groupBy { x -> x.mold }.mapNotNull { x ->
                            PlanSummaryDetailModel(
                                type = if (frame1 != Frame1.MU) "" else x.key,
                                planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                                    .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                                        val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                        data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                            .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                        data
                                    }
                            )
                        }.firstOrNull()
                        if (dataMold == null) {
                            dataMold = PlanSummaryDetailModel(
                                type = if (frame1 != Frame1.MU) "" else mold,
                                planSummaryData = mutableListOf(PlanDataByProcessModel(title = PlanTitle.PLAN, titleKey = PlanTitle.PLAN_KEY, quantityByCalendars = listOf()))
                            )
                        }
                        ducLo.details!!.add(dataMold)
                    }
                }

                dataSummary.removeAll(dataDucLo)
                dataSummary.add(ducLo)
            }

            val dataInMach = dataSummary.filter { x -> x.frame1 == frame1 && (x.processConvertCode == ProcessConvertCode.TAN || x.processConvertCode == ProcessConvertCode.ZEN) }
            if (dataInMach.isNotEmpty()) {
                val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.IN_MACH }

                val inMach = PlanSummaryModel(
                    processName = process?.description,
                    processNameJp = process?.descriptionJp,
                    processConvertCode = "${ProcessConvertCode.TAN}/${ProcessConvertCode.ZEN}",
                    processSequence = process?.sortOrder?.toInt(),
                    frame1 = dataInMach.first().frame1,
                    processCode = dataInMach.first().processCode,
                    unit = dataInMach.first().unit,
                    details = mutableListOf(
                        PlanSummaryDetailModel(
                            type = CommonUtils.getMessage(""),
                            planSummaryData = dataInMach.asSequence().mapNotNull { x -> x.details }.flatten().mapNotNull { x -> x.planSummaryData }.flatten()
                                .groupBy { x -> Pair(x.titleKey, x.title) }.map { x ->
                                    val data = PlanDataByProcessModel(title = x.key.second, titleKey = x.key.first)
                                    data.quantityByCalendars = x.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                        .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                    data
                                }.toList()
                        )
                    )
                )

                dataSummary.removeAll(dataInMach)
                dataSummary.add(inMach)
            }

            val dataInLo = dataSummary.filter { x -> x.frame1 == frame1 && (x.processConvertCode == ProcessConvertCode.HP_ALL || x.processConvertCode == ProcessConvertCode.HP) }
            if (dataInLo.isNotEmpty()) {
                val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.IN_LO }

                val inLo = PlanSummaryModel(
                    processName = process?.description,
                    processNameJp = process?.descriptionJp,
                    processConvertCode = "${ProcessConvertCode.HP_ALL}/${ProcessConvertCode.HP}",
                    processSequence = process?.sortOrder?.toInt(),
                    frame1 = dataInLo.first().frame1,
                    processCode = dataInLo.first().processCode,
                    unit = dataInLo.first().unit,
                    details = mutableListOf(
                        PlanSummaryDetailModel(
                            type = CommonUtils.getMessage(""),
                            planSummaryData = dataInLo.asSequence().mapNotNull { x -> x.details }.flatten().mapNotNull { x -> x.planSummaryData }.flatten()
                                .groupBy { x -> Pair(x.titleKey, x.title) }.map { x ->
                                    val data = PlanDataByProcessModel(title = x.key.second, titleKey = x.key.first)
                                    data.quantityByCalendars = x.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                        .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                    data
                                }.toList()
                        )
                    )
                )
                dataSummary.removeAll(dataInLo)
                dataSummary.add(inLo)
            }

            val dataGhepLop = dataSummary.filter { x -> x.frame1 == frame1 && (!x.processConvertCode.isNullOrEmpty() && x.processConvertCode!!.startsWith(ProcessConvertCode.M)) }
            if (dataGhepLop.isNotEmpty()) {
                val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.GHEP_LOP_SUM }

                val detailsList = if (frame1 != Frame1.MU) {
                    mutableListOf(
                        PlanSummaryDetailModel(
                            type = "",
                            planSummaryData = dataGhepLop.asSequence().mapNotNull { x -> x.details }.flatten().mapNotNull { x -> x.planSummaryData }.flatten()
                                .groupBy { x -> Pair(x.titleKey, x.title) }.map { x ->
                                    val data = PlanDataByProcessModel(title = x.key.second, titleKey = x.key.first)
                                    data.quantityByCalendars = x.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                        .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                    data
                                }.toList()
                        )
                    )
                } else {
                    mutableListOf()
                }

                val ghepLop = PlanSummaryModel(
                    processName = process?.description,
                    processNameJp = process?.descriptionJp,
                    processConvertCode = "${ProcessConvertCode.M_ALL}/${ProcessConvertCode.M_ANY}",
                    processSequence = process?.sortOrder?.toInt(),
                    frame1 = dataGhepLop.first().frame1,
                    processCode = dataGhepLop.first().processCode,
                    unit = dataGhepLop.first().unit,
                    details = detailsList
                )

                if (frame1 == Frame1.MU) {
                    val mGAN = dataExportFlattens.filter { x ->
                        x.processStatisticCode == ProcessStatisticCode.GHEPLOP_GIAAPNHIET
                    }.groupBy { x -> x.processConvertCode }.mapNotNull { x ->
                        PlanSummaryDetailModel(
                            type = "Ghép lớp gia áp nhiệt",
                            planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                                .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                                    val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                    data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                        .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                    data
                                }
                        )
                    }.firstOrNull()
                    if (mGAN != null) {
                        ghepLop.details!!.add(mGAN)
                    }

                    val mGLT = dataExportFlattens.filter { x ->
                        !x.processConvertCode.isNullOrEmpty()
                            && x.processStatisticCode != ProcessStatisticCode.GHEPLOP_GIAAPNHIET
                            && x.processConvertCode!!.startsWith(ProcessConvertCode.M)
                    }.groupBy { x -> x.processConvertCode }.mapNotNull { x ->
                        PlanSummaryDetailModel(
                            type = "Ghép lớp thường",
                            planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                                .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                                    val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                    data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                        .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                    data
                                }
                        )
                    }.firstOrNull()
                    if (mGLT != null) {
                        ghepLop.details!!.add(mGLT)
                    }
                }

                dataSummary.removeAll(dataGhepLop)
                dataSummary.add(ghepLop)
            }

            if (frame1 == Frame1.MU) {
                val dataThaoKhungCsp = dataSummary.filter { x -> x.frame1 == frame1 && (x.processConvertCode == ProcessConvertCode.TK) }
                if (dataThaoKhungCsp.isNotEmpty()) {
                    val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.TK_CSP }

                    val thaoKhungCsp = PlanSummaryModel(
                        processName = process?.description,
                        processNameJp = process?.descriptionJp,
                        processConvertCode = ProcessConvertCode.TK,
                        processSequence = process?.sortOrder?.toInt(),
                        frame1 = frame1,
                        processCode = dataThaoKhungCsp.first().processCode,
                        unit = dataThaoKhungCsp.first().unit,
                        details = mutableListOf()
                    )

                    val csp = dataExportFlattens.filter { x ->
                        !x.processConvertCode.isNullOrEmpty()
                            && x.processCode == "214220"
                    }.groupBy { x -> x.processConvertCode }.mapNotNull { x ->
                        PlanSummaryDetailModel(
                            type = "",
                            planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                                .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                                    val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                    data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                        .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                    data
                                }
                        )
                    }.firstOrNull()
                    if (csp != null) {
                        thaoKhungCsp.details!!.add(csp)
                    }
                    if (thaoKhungCsp.details.isNullOrEmpty()) {
                        val planSummaryData: MutableList<PlanDataByProcessModel> = mutableListOf()
                        planSummaryData.add(PlanDataByProcessModel(ProcessPlan.PROCESS, ProcessPlan.PROCESS, listOf()))
                        thaoKhungCsp.details?.add(PlanSummaryDetailModel(type = "", planSummaryData = planSummaryData))
                    }
                    dataSummary.removeAll(dataThaoKhungCsp)
                    dataSummary.add(thaoKhungCsp)
                }

                val dataSnaps = dataSummary.filter { x -> x.frame1 == frame1 && x.processConvertCode == ProcessConvertCode.SNAP }
                if (dataSnaps.isNotEmpty()) {
                    var moldByFrame1s = (appSettingRep.findByKey("${KeyAppSetting.MOLD_BY_FRAME1}_${frame1}")?.value?.split(",")
                        ?: Mold.DATA_BY_FRAME1(frame1)).filter { x -> request.mold.isNullOrEmpty() || x == request.mold }
                    val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.SNAP }
                    val listMoldRequest: MutableList<String> = mutableListOf()
                    if (!request.mold.isNullOrEmpty()) {
                        listMoldRequest.add(request.mold!!)
                        moldByFrame1s = listMoldRequest
                    }
                    val snap = PlanSummaryModel(
                        processName = process?.description,
                        processNameJp = process?.descriptionJp,
                        processConvertCode = ProcessStatisticCode.SNAP,
                        processSequence = process?.sortOrder?.toInt(),
                        details = mutableListOf(),
                        frame1 = dataSnaps.first().frame1,
                        processCode = dataSnaps.first().processCode,
                        unit = dataSnaps.first().unit
                    )
                    if (moldByFrame1s.isNotEmpty()) {
                        for (mold in moldByFrame1s) {
                            var dataSnap = dataExportFlattens.filter { x ->
                                x.mold == mold && x.processConvertCode == ProcessConvertCode.SNAP
                            }.groupBy { x -> x.mold }.mapNotNull { x ->
                                PlanSummaryDetailModel(
                                    type = if (frame1 != Frame1.MU) "" else x.key,
                                    planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                                        .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                                            val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                            data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                                .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                            data
                                        }
                                )
                            }.firstOrNull()
                            if (dataSnap == null) {
                                dataSnap = PlanSummaryDetailModel(
                                    type = if (frame1 != Frame1.MU) "" else mold,
                                    planSummaryData = mutableListOf(PlanDataByProcessModel(title = PlanTitle.PLAN, titleKey = PlanTitle.PLAN_KEY, quantityByCalendars = listOf()))
                                )
                            }
                            snap.details!!.add(dataSnap)
                        }
                    }

                    dataSummary.removeAll(dataSnaps)
                    dataSummary.add(snap)
                }
            } else {
                val dataThaoKhung = dataSummary.filter { x -> x.frame1 == frame1 && (x.processConvertCode == ProcessConvertCode.TK) }
                dataSummary.removeAll(dataThaoKhung)
            }
        }

        response.data = dataSummary.sortedBy { x -> x.processSequence }
        return response

    }

    private fun getDataExportExcelEquipment(planProducts: List<PlanProduct>, request: PlanSearchRequest): List<PlanExportExcelModel> {
        val planProductIds = planProducts.mapNotNull { x -> x.id }

        val planProcesses = planProcessRep.getListPlanProcess(planProductIds, request.draftWorkPlan ?: false, request.processGroups)
        var parentPlanProcesses = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }.sortedBy { x -> x.planProductId }
        val childrenPlanProcesses = planProcesses.filter { x -> x.parentId != null }

        var planProcessIds = parentPlanProcesses.mapNotNull { x -> x.id }
        val planDetails = planDetailRep.getPlanDetail(
            planProcessIds, request.startDate!!, request.endDate!!,
            request.inventoryWorkPlan ?: false, request.draftWorkPlan ?: false
        )

        planProcessIds = planDetails.mapNotNull { x -> x.planProcessId }
        parentPlanProcesses = parentPlanProcesses.filter { x -> planProcessIds.any { m -> m == x.id } }

        val data = mutableListOf<PlanExportExcelModel>()

        for (planProduct in planProducts) {
            val parentPlanProcess = parentPlanProcesses.filter { it.planProductId == planProduct.id }
            val childrenPlanProcess = childrenPlanProcesses.filter { it.planProductId == planProduct.id }

            val productExport = PlanExportExcelModel(
                id = planProduct.id,
                productName = planProduct.productName,
                frame_1 = planProduct.frame_1,
                mold = planProduct.mold,
                pcsSh = planProduct.pcsSh,
                blockSh = planProduct.blockSh
            )
            productExport.productPlanDetails = parentPlanProcess.filter { x -> x.planProductId == planProduct.id }.map { x ->
                val productPlan = ProductPlanDetailModel(
                    frame_1 = planProduct.frame_1,
                    mold = planProduct.mold,
                    layerCode = x.layerCode,
                    processCode = x.processCode,
                    processName = x.processName,
                    processNameJp = x.processNameJp,
                    completionRate = x.completionRate,
                    processConvertCode = x.processConvertCode,
                    processStatisticCode = x.processStatisticCode,
                    processGroup = x.processGroup,
                    processSequence = x.processSequence,
                    unit = x.unit,
                    inventory = x.inventory
                )
                productPlan.processChildren = childrenPlanProcess.filter { m -> m.parentId == x.id && m.layerCode == x.layerCode }.map { m ->
                    ProcessChildrenModel(
                        layerCode = m.layerCode,
                        processCode = m.processCode,
                        processName = m.processName,
                        inventory = m.inventory
                    )
                }
                productPlan.sumInventory = (productPlan.processChildren?.sumOf { m -> m.inventory ?: 0 } ?: 0) + (productPlan.inventory ?: 0)

                val planDetailByProcess = planDetails.filter { m -> m.planProcessId == x.id }
                val planDetail = planDetailByProcess.filter { t -> t.title == PlanTitle.PLAN_KEY }.map { t ->
                    KeyValueResponse(
                        DateTimeHelper.toString(DateTimeHelper.toTimeZone7(t.planDate)!!, DateTimeFormat.yyyyMMdd),
                        if (x.unit == ProcessUnit.BLOCK) t.blockQuantity?.toString() else t.sheetQuantity?.toString()
                    )
                }


                val planData = mutableListOf<PlanDataByProcessModel>()
                planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN, titleKey = PlanTitle.PLAN_KEY, quantityByCalendars = planDetail))

                productPlan.planData = planData
                productPlan
            }.sortedWith(compareBy<ProductPlanDetailModel> { x -> x.layerCode?.toInt() }.thenBy { x -> x.processSequence })
            data.add(productExport)
        }

        val response = data.groupBy { it.productName }.map { prod ->
            val product = prod.value.first()
            val res = PlanExportExcelModel(
                productName = product.productName,
                frame_1 = product.frame_1,
                mold = product.mold,
                pcsSh = product.pcsSh,
                blockSh = product.blockSh,
                productPlanDetails = prod.value.mapNotNull { it.productPlanDetails }.flatten().groupBy { Pair(it.processCode, it.layerCode) }.map { item ->
                    val process = item.value.first()
                    val rs = ProductPlanDetailModel(
                        frame_1 = process.frame_1,
                        mold = process.mold,
                        layerCode = item.key.second,
                        processCode = item.key.first,
                        processName = process.processName,
                        processNameJp = process.processNameJp,
                        completionRate = process.completionRate,
                        processConvertCode = process.processConvertCode,
                        processStatisticCode = process.processStatisticCode,
                        processGroup = process.processGroup,
                        processSequence = process.processSequence,
                        inventory = item.value.sumOf { it.inventory ?: 0 },
                        sumInventory = item.value.sumOf { it.sumInventory ?: 0 },
                        processChildren = process.processChildren,
                        unit = process.unit,
                        planData = item.value.asSequence().mapNotNull { it.planData }.flatten().groupBy { it.titleKey }.map { x ->
                            PlanDataByProcessModel(
                                title = x.value.first().title,
                                titleKey = x.key,
                                quantityByCalendars = x.value.mapNotNull { it.quantityByCalendars }.flatten().groupBy { it.key }.map { m ->
                                    KeyValueResponse(
                                        m.key,
                                        m.value.sumOf { it.value?.toInt() ?: 0 }.toString(),
                                    )
                                }
                            )
                        }
                    )
                    rs
                }
            )
            res
        }
        return response
    }

    fun importExcelEquipment(file: MultipartFile): BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1
        var count = 0
        val total = sheet.lastRowNum - rowIndex

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val equipmentProductivity = EquipmentProductivity(
                grpProcess = ExcelHelper.getCellValue(row, 1).let { if (it.length > 5) it.substring(0, 5) else it },
                processName = ExcelHelper.getCellValue(row, 11),
                mold = ExcelHelper.getCellValue(row, 2),
                task = BigDecimal(ExcelHelper.getCellValue(row, 6)),
                time = ExcelHelper.getCellValue(row, 4).run { if (endsWith(".0")) substring(0, length - 2) else this }.toInt(),
                count = BigDecimal(ExcelHelper.getCellValue(row, 5)),
                sltbSet = NumberHelper.truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 5)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                capSet = NumberHelper.truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 5)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                sltbBlock = NumberHelper.truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 8)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 5)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                capBlock = NumberHelper.truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 8)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 5)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                blockSh = BigDecimal(ExcelHelper.getCellValue(row, 8)),
                frame_1 = ExcelHelper.getCellValue(row, 0),
                sltbHour = NumberHelper.truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                capHour = NumberHelper.truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                sltbSheet = NumberHelper.truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                capSheet = NumberHelper.truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 4)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 3)) *
                        BigDecimal(ExcelHelper.getCellValue(row, 7))
                ),
                operatingRate = NumberHelper.truncateDecimal(
                    BigDecimal(ExcelHelper.getCellValue(row, 3)) * BigDecimal(100)
                ),
                sheetHour_100 = NumberHelper.truncateDecimal(BigDecimal(ExcelHelper.getCellValue(row, 7))),
                quantityMachine = ExcelHelper.getCellValue(row, 9).run { if (endsWith(".0")) substring(0, length - 2) else this }.toInt(),
                processCode = ExcelHelper.getCellValue(row, 10).let { if (it.length > 6) it.substring(0, 6) else it },
                description = "insert"
            )

            equipmentProductivityRep.add(equipmentProductivity)
            count++
        }
        return BaseResponse(null, CommonUtils.getMessage("Insert Ok", arrayOf(count, total + 1)))
    }

    private fun generateExcelRowProcessEquipment(
        workbook: Workbook,
        sheet: Sheet,
        rowNumber: Int,
        style: CellStyle,
        data: EquipmentProductivityModel,
        styleCollections: MutableList<CellStyleModel>
    ): Int {
        var rowIndex = rowNumber
        var dataRow = ExcelHelper.createRow(sheet, rowIndex)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 1, style, data.processName, isBold = false, isAlignCenter = true, isBorderLeft = true, isBorderRight = true, isBorderTop = true, isBorderBottom = false)
        styleCollections.add(CellStyleModel(1, dataRow.getCell(1).cellStyle, PlanStyleKey.PLAN_SUMMARY_FIRST_ROW))

        rowIndex++
        dataRow = ExcelHelper.createRow(sheet, rowIndex)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 1, style, data.processNameJp, isBold = false, isAlignCenter = true, isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false)
        styleCollections.add(CellStyleModel(1, dataRow.getCell(1).cellStyle, PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW))

        rowIndex++
        dataRow = ExcelHelper.createRow(sheet, rowIndex)
        ExcelHelper.setCellValueCustom(workbook, dataRow, 1, style, data.processConvertCode, isBold = false, isAlignCenter = true, isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false)

        rowIndex++
        var processDetailSize = (data.processDetail?.size ?: 1)
        if (processDetailSize == 0) processDetailSize = 1
        for (i in 3 until (processDetailSize * 4)) {
            dataRow = ExcelHelper.createRow(sheet, rowIndex)

            if (i == (processDetailSize * 4) - 1) {
                ExcelHelper.setCellValueCustom(
                    workbook, dataRow, 1, style, "", isBold = false, isAlignCenter = true,
                    isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = true
                )
                styleCollections.add(CellStyleModel(1, dataRow.getCell(1).cellStyle, PlanStyleKey.PLAN_SUMMARY_END_ROW))
            } else {
                ExcelHelper.setCellValueCustom(
                    workbook, dataRow, 1, style, "", isBold = false, isAlignCenter = true,
                    isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false
                )
            }
            rowIndex++
        }
        return rowIndex
    }

    private fun generateExcelRowProcessEquipment(
        sheet: Sheet,
        rowNumber: Int,
        data: EquipmentProductivityModel,
        styleCollections: MutableList<CellStyleModel>
    ): Int {
        var rowIndex = rowNumber
        var dataRow = ExcelHelper.createRow(sheet, rowIndex)
        ExcelHelper.setCellValue(dataRow, 1, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle, data.processName)

        rowIndex++
        dataRow = ExcelHelper.createRow(sheet, rowIndex)
        ExcelHelper.setCellValue(dataRow, 1, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle, data.processNameJp)

        rowIndex++
        dataRow = ExcelHelper.createRow(sheet, rowIndex)
        ExcelHelper.setCellValue(dataRow, 1, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle, data.processConvertCode)

        rowIndex++
        var processDetailSize = (data.processDetail?.size ?: 1)
        if (processDetailSize == 0) processDetailSize = 1
        for (i in 3 until (processDetailSize * 4)) {
            dataRow = ExcelHelper.createRow(sheet, rowIndex)
            val st = if (i == (processDetailSize * 4) - 1) {
                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
            } else {
                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
            }
            ExcelHelper.setCellValue(dataRow, 1, st, "")
            rowIndex++
        }
        return rowIndex
    }

    //endregion

    //region EXCEL

    fun exportExcel(request: PlanSearchRequest): FileContentModel {
        if (request.startDate == null || request.endDate == null) throw BusinessException(CommonUtils.getMessage("plan.invalidTime"))

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        val columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(request.startDate)!!, DateTimeHelper.toTimeZone7(request.endDate)!!, holidayCalenders)

        return when (request.activeTab) {
            PlanActiveTab.PLAN -> exportPlanInfo(request, columns)
            PlanActiveTab.SUM -> exportPlanSummary(request, columns)
            PlanActiveTab.SUM_ML -> {
                request.frame_1 = Frame1.ML
                exportPlanSummary(request, columns)
            }
            PlanActiveTab.SUM_MU -> {
                request.frame_1 = Frame1.MU
                exportPlanSummary(request, columns)
            }
            PlanActiveTab.SUM_SWR -> {
                request.frame_1 = Frame1.SWR
                exportPlanSummary(request, columns)
            }
            PlanActiveTab.EQUIPMENT -> exportEquipment(request, columns)
            else -> FileContentModel()
        }
    }

    private fun exportPlanInfo(request: PlanSearchRequest, columns: List<CalendarResponse>): FileContentModel {
        val planProducts = planProductRep.getListPlanProduct(request)
        val dataExports = getDataExportExcel(
            planProducts, request.startDate!!, request.endDate!!, columns,
            request.inventoryWorkPlan ?: false, request.draftWorkPlan ?: false, request.processGroups
        )
        if (dataExports.isEmpty()) throw BusinessException(CommonUtils.getMessage("excel.export.noData"))

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportPlanTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }

        generateDataSheetPlan(workbook, columns, planProducts, dataExports, request)

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportPlan", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()
        return response
    }

    private fun exportPlanSummary(request: PlanSearchRequest, columns: List<CalendarResponse>): FileContentModel {
        val planProducts = planProductRep.getListPlanProduct(request)
        val dataExports = getDataExportExcel(
            planProducts, request.startDate!!, request.endDate!!, columns,
            request.inventoryWorkPlan ?: false, request.draftWorkPlan ?: false, request.processGroups, true
        )
        if (dataExports.isEmpty()) throw BusinessException(CommonUtils.getMessage("excel.export.noData"))

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportPlanSummaryTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }

        val headerStyle = workbook.getSheetAt(0).getRow(0).getCell(0).cellStyle

        val dataSummary = mutableListOf<Pair<String, List<PlanSummaryModel>?>>()

        if (request.frame_1.isNullOrEmpty()) {
            val frame1s = commonCategoryRep.getByType(listOf(MasterDataType.KHUNG_1))
            for (frame in frame1s) {
                request.frame_1 = frame.value
                val summaryByFrame = getPlanSummary(request, dataExports.filter { x -> x.frame_1 == frame.value }.toMutableList(), true)
                if (!summaryByFrame.data.isNullOrEmpty()) {
                    dataSummary.add(Pair(frame.value ?: "", summaryByFrame.data))
                }
            }
            request.frame_1 = null
            val planSummary = getPlanSummary(request, dataExports.toMutableList(), true)
            dataSummary.add(Pair("TOTAL", planSummary.data))
        } else {
            val planSummary = getPlanSummary(request, dataExports.toMutableList(), true)
            dataSummary.add(Pair(request.frame_1!!, planSummary.data))
        }
        generateDataSheetSummary(workbook, columns, dataSummary, headerStyle, request.frame_1)

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()
        val response = FileContentModel(
            fileName = CommonUtils.getMessage(
                "fileName.exportPlanSummary",
                arrayOf(
                    if (request.frame_1.isNullOrEmpty()) "" else "_${request.frame_1}",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss"))
                )
            ),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()
        return response
    }

    private fun exportEquipment(request: PlanSearchRequest, columns: List<CalendarResponse>): FileContentModel {
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportEquipmentConfigTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }

        val headerStyle = workbook.getSheetAt(0).getRow(0).getCell(0).cellStyle

        val dataEquipment = getEquipmentProductivityPlan(request)
        generateDataSheetEquipment(workbook, columns, dataEquipment?.data, headerStyle)

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportEquipmentConfig", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()
        return response
    }

    private fun generateDataSheetPlan(
        workbook: Workbook,
        columns: List<CalendarResponse>,
        planProducts: List<PlanProduct>,
        dataExports: List<PlanExportExcelModel>,
        request: PlanSearchRequest
    ) {
        val sheet = workbook.getSheetAt(0)
        val headerRow = sheet.getRow(0)
        var headerCol = 14
        val headerStyle = headerRow.getCell(0).cellStyle
        for (col in columns) {
            ExcelHelper.setCellValueWithCalendar(workbook, headerRow, headerCol, headerStyle, col.value, col.isHoliday)
            headerCol++
        }

        // setting style
        val styleCommon = ExcelHelper.getCellStyleCommon(workbook)
        styleCommon.alignment = HorizontalAlignment.CENTER
        val fontTemplate = workbook.getFontAt(styleCommon.fontIndex)

        val startRowStyle = workbook.createCellStyle()
        startRowStyle.cloneStyleFrom(styleCommon)
        startRowStyle.fillForegroundColor = IndexedColors.PALE_BLUE.index
        startRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        startRowStyle.borderBottom = BorderStyle.NONE

        val firstRowStyle = workbook.createCellStyle()
        firstRowStyle.cloneStyleFrom(styleCommon)
        firstRowStyle.borderBottom = BorderStyle.NONE

        val middleRowStyle = workbook.createCellStyle()
        middleRowStyle.cloneStyleFrom(styleCommon)
        middleRowStyle.borderTop = BorderStyle.NONE
        middleRowStyle.borderBottom = BorderStyle.NONE

        val endRowStyle = workbook.createCellStyle()
        endRowStyle.cloneStyleFrom(styleCommon)
        endRowStyle.borderTop = BorderStyle.NONE

        val planStartRowStyle = workbook.createCellStyle()
        planStartRowStyle.cloneStyleFrom(styleCommon)
        planStartRowStyle.fillForegroundColor = IndexedColors.PALE_BLUE.index
        planStartRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        planStartRowStyle.borderBottom = BorderStyle.DOTTED
        planStartRowStyle.alignment = HorizontalAlignment.RIGHT

        val planRowStyle = workbook.createCellStyle()
        planRowStyle.cloneStyleFrom(styleCommon)
        planRowStyle.borderBottom = BorderStyle.DOTTED
        planRowStyle.alignment = HorizontalAlignment.RIGHT

        val titlePlanStartRowStyle = workbook.createCellStyle()
        titlePlanStartRowStyle.cloneStyleFrom(styleCommon)
        titlePlanStartRowStyle.fillForegroundColor = IndexedColors.PALE_BLUE.index
        titlePlanStartRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        titlePlanStartRowStyle.borderBottom = BorderStyle.DOTTED

        val titlePlanStyle = workbook.createCellStyle()
        titlePlanStyle.cloneStyleFrom(styleCommon)
        titlePlanStyle.borderBottom = BorderStyle.DOTTED

        val redFont = workbook.createFont()
        redFont.fontName = fontTemplate.fontName
        redFont.fontHeightInPoints = fontTemplate.fontHeightInPoints
        redFont.color = IndexedColors.RED.index

        val actualRowStyle = workbook.createCellStyle()
        actualRowStyle.cloneStyleFrom(styleCommon)
        actualRowStyle.borderBottom = BorderStyle.DOTTED
        actualRowStyle.alignment = HorizontalAlignment.RIGHT
        actualRowStyle.setFont(redFont)

        val titleActualStyle = workbook.createCellStyle()
        titleActualStyle.cloneStyleFrom(styleCommon)
        titleActualStyle.borderBottom = BorderStyle.DOTTED
        titleActualStyle.setFont(redFont)

        val blueFont = workbook.createFont()
        blueFont.fontName = fontTemplate.fontName
        blueFont.fontHeightInPoints = fontTemplate.fontHeightInPoints
        blueFont.color = IndexedColors.BLUE.index

        val accumulationRowStyle = workbook.createCellStyle()
        accumulationRowStyle.cloneStyleFrom(styleCommon)
        accumulationRowStyle.borderTop = BorderStyle.DOTTED
        accumulationRowStyle.alignment = HorizontalAlignment.RIGHT
        accumulationRowStyle.setFont(blueFont)

        val titleAccumulationStyle = workbook.createCellStyle()
        titleAccumulationStyle.cloneStyleFrom(styleCommon)
        titleAccumulationStyle.borderTop = BorderStyle.DOTTED
        titleAccumulationStyle.setFont(blueFont)

        val holidayStyle = workbook.createCellStyle()
        holidayStyle.cloneStyleFrom(styleCommon)
        holidayStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        holidayStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        holidayStyle.alignment = HorizontalAlignment.RIGHT

        val holidayStartRowStyle = workbook.createCellStyle()
        holidayStartRowStyle.cloneStyleFrom(styleCommon)
        holidayStartRowStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        holidayStartRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        holidayStartRowStyle.alignment = HorizontalAlignment.RIGHT
        holidayStartRowStyle.borderBottom = BorderStyle.DOTTED

        val holidayActualRowStyle = workbook.createCellStyle()
        holidayActualRowStyle.cloneStyleFrom(styleCommon)
        holidayActualRowStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        holidayActualRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        holidayActualRowStyle.alignment = HorizontalAlignment.RIGHT
        holidayActualRowStyle.borderBottom = BorderStyle.DOTTED
        holidayActualRowStyle.setFont(redFont)

        val holidayAccumulationRowStyle = workbook.createCellStyle()
        holidayAccumulationRowStyle.cloneStyleFrom(styleCommon)
        holidayAccumulationRowStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        holidayAccumulationRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        holidayAccumulationRowStyle.alignment = HorizontalAlignment.RIGHT
        holidayAccumulationRowStyle.borderTop = BorderStyle.DOTTED
        holidayAccumulationRowStyle.setFont(blueFont)

        val negativeNumStyle = workbook.createCellStyle()
        negativeNumStyle.cloneStyleFrom(styleCommon)
        negativeNumStyle.alignment = HorizontalAlignment.RIGHT
        negativeNumStyle.setFont(redFont)

        val holidayWithNegativeNumStyle = workbook.createCellStyle()
        holidayWithNegativeNumStyle.cloneStyleFrom(styleCommon)
        holidayWithNegativeNumStyle.setFont(redFont)
        holidayWithNegativeNumStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        holidayWithNegativeNumStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        holidayWithNegativeNumStyle.alignment = HorizontalAlignment.RIGHT

        val firstRowWithNegativeNumStyle = workbook.createCellStyle()
        firstRowWithNegativeNumStyle.cloneStyleFrom(styleCommon)
        firstRowWithNegativeNumStyle.setFont(redFont)
        firstRowWithNegativeNumStyle.fillForegroundColor = IndexedColors.PALE_BLUE.index
        firstRowWithNegativeNumStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        firstRowWithNegativeNumStyle.alignment = HorizontalAlignment.RIGHT

        val numberStyleCommon = workbook.createCellStyle()
        numberStyleCommon.cloneStyleFrom(styleCommon)
        numberStyleCommon.alignment = HorizontalAlignment.RIGHT

        //

        var rowNumber = 1
        val productNames = planProducts.map { it.productName }.distinct()
        for (productName in productNames) {
            val dataExport = dataExports.find { x -> x.productName == productName }
            if (dataExport == null || dataExport.productPlanDetails.isNullOrEmpty()) continue
            var isNextProduct = true
            for (planProcess in dataExport.productPlanDetails!!) {
                var rowProcessIndex = rowNumber
                val styleProcess = if (isNextProduct) startRowStyle else firstRowStyle
                var dataRow = ExcelHelper.createRow(sheet, rowProcessIndex)

                ExcelHelper.setCellValue(dataRow, 6, styleProcess, planProcess.layerCode)
                ExcelHelper.setCellValue(dataRow, 7, styleProcess, planProcess.processGroup)
                ExcelHelper.setCellValue(dataRow, 8, styleProcess, planProcess.processCode)
                ExcelHelper.setCellValue(dataRow, 9, styleProcess, planProcess.processName)
                ExcelHelper.setCellValue(dataRow, 10, styleProcess, "")
                ExcelHelper.setCellValue(dataRow, 11, styleProcess, if (planProcess.completionRate != null) "${planProcess.completionRate?.toString()} %" else "")

                rowProcessIndex++
                if (!planProcess.processChildren.isNullOrEmpty()) {
                    for (children in planProcess.processChildren!!) {
                        dataRow = ExcelHelper.createRow(sheet, rowProcessIndex)

                        ExcelHelper.setCellValue(dataRow, 6, middleRowStyle, planProcess.layerCode)
                        ExcelHelper.setCellValue(dataRow, 7, middleRowStyle, "")
                        ExcelHelper.setCellValue(dataRow, 8, middleRowStyle, children.processCode)
                        ExcelHelper.setCellValue(dataRow, 9, middleRowStyle, children.processName)
                        ExcelHelper.setCellValue(dataRow, 10, middleRowStyle, children.layerCode)
                        ExcelHelper.setCellValue(dataRow, 11, middleRowStyle, "")
                        rowProcessIndex++
                    }
                }

                var rowDataIndex = rowNumber
                for ((iItem, item) in planProcess.planData!!.withIndex()) {
                    val style = if (isNextProduct) startRowStyle
                    else if (iItem == 0) firstRowStyle
                    else if (iItem == planProcess.planData!!.size - 1) endRowStyle
                    else middleRowStyle

                    dataRow = ExcelHelper.createRow(sheet, rowDataIndex)

                    ExcelHelper.setCellValue(dataRow, 0, style, dataExport.productName?.substring(dataExport.productName!!.length - 7, dataExport.productName!!.length))
                    ExcelHelper.setCellValue(dataRow, 1, style, dataExport.productName)
                    ExcelHelper.setCellValue(dataRow, 2, style, dataExport.pcsSh?.toString())
                    ExcelHelper.setCellValue(dataRow, 3, style, dataExport.blockSh?.toString())
                    ExcelHelper.setCellValue(dataRow, 4, style, dataExport.frame_1)
                    ExcelHelper.setCellValue(dataRow, 5, style, dataExport.mold)

                    val titleStyle = if (isNextProduct) titlePlanStartRowStyle
                    else if (item.titleKey == PlanTitle.PLAN_KEY) titlePlanStyle
                    else if (item.titleKey == PlanTitle.ACTUAL_KEY) titleActualStyle
                    else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) titleAccumulationStyle
                    else styleCommon
                    ExcelHelper.setCellValue(dataRow, 12, titleStyle, "${planProcess.processConvertCode} ${item.title}")

                    val totalStyle = if (isNextProduct) planStartRowStyle
                    else {
                        if (item.titleKey == PlanTitle.PLAN_KEY) planRowStyle
                        else if (item.titleKey == PlanTitle.ACTUAL_KEY) actualRowStyle
                        else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) accumulationRowStyle
                        else numberStyleCommon
                    }
                    ExcelHelper.setCellValue(dataRow, 13, totalStyle, NumberHelper.formatNumber(item.total?.toInt()))

                    var colIndex = 14
                    for (col in columns) {
                        val value = item.quantityByCalendars?.find { x -> x.key == col.key }?.value
                        val st = if (col.isHoliday) {
                            if ((value?.toIntOrNull() ?: 0) < 0) {
                                holidayWithNegativeNumStyle
                            } else {
                                if (item.titleKey == PlanTitle.PLAN_KEY) holidayStartRowStyle
                                else if (item.titleKey == PlanTitle.ACTUAL_KEY) holidayActualRowStyle
                                else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) holidayAccumulationRowStyle
                                else holidayStyle
                            }
                        } else {
                            if (isNextProduct) planStartRowStyle
                            else {
                                if ((value?.toIntOrNull() ?: 0) < 0) {
                                    negativeNumStyle
                                } else {
                                    if (item.titleKey == PlanTitle.PLAN_KEY) planRowStyle
                                    else if (item.titleKey == PlanTitle.ACTUAL_KEY) actualRowStyle
                                    else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) accumulationRowStyle
                                    else numberStyleCommon
                                }
                            }
                        }
                        ExcelHelper.setCellValue(dataRow, colIndex, st, NumberHelper.formatNumber(value?.toIntOrNull()))
                        colIndex++
                    }

                    rowDataIndex++
                    isNextProduct = false
                }
                if (rowDataIndex > rowProcessIndex) {
                    for (i in rowProcessIndex until rowDataIndex) {
                        val style = if (i == rowDataIndex - 1) endRowStyle else middleRowStyle
                        dataRow = ExcelHelper.createRow(sheet, i)
                        ExcelHelper.setCellValue(dataRow, 6, style, planProcess.layerCode)
                        ExcelHelper.setCellValue(dataRow, 7, style, "")
                        ExcelHelper.setCellValue(dataRow, 8, style, "")
                        ExcelHelper.setCellValue(dataRow, 9, style, "")
                        ExcelHelper.setCellValue(dataRow, 10, style, "")
                        ExcelHelper.setCellValue(dataRow, 11, style, "")
                    }
                    rowNumber = rowDataIndex
                } else {
                    if (rowDataIndex == rowProcessIndex) rowNumber = rowDataIndex
                    else {
                        for (i in rowDataIndex until rowProcessIndex) {
                            val style = if (i == rowProcessIndex - 1) endRowStyle else middleRowStyle
                            dataRow = ExcelHelper.createRow(sheet, i)
                            ExcelHelper.setCellValue(dataRow, 0, style, dataExport.productName?.substring(dataExport.productName!!.length - 7, dataExport.productName!!.length))
                            ExcelHelper.setCellValue(dataRow, 1, style, dataExport.productName)
                            ExcelHelper.setCellValue(dataRow, 2, style, dataExport.pcsSh?.toString())
                            ExcelHelper.setCellValue(dataRow, 3, style, dataExport.blockSh?.toString())
                            ExcelHelper.setCellValue(dataRow, 4, style, dataExport.frame_1)
                            ExcelHelper.setCellValue(dataRow, 5, style, dataExport.mold)
                            ExcelHelper.setCellValue(dataRow, 12, styleCommon, "")
                            ExcelHelper.setCellValue(dataRow, 13, styleCommon, "")

                            var colIndex = 14
                            for (col in columns) {
                                ExcelHelper.setCellValue(dataRow, colIndex, (if (col.isHoliday) holidayStyle else styleCommon), "")
                                colIndex++
                            }
                        }
                        rowNumber = rowProcessIndex
                    }
                }
            }
        }

        val dataSummary = mutableListOf<Pair<String, List<PlanSummaryModel>?>>()
        val dataExportMapping = dataExports.map { item ->
            item.productPlanDetails = item.productPlanDetails?.map { x ->
                x.planData = x.planData?.filter { it.titleKey == PlanTitle.PLAN_KEY || it.titleKey == PlanTitle.ACTUAL_KEY }
                x
            }
            item
        }
        if (request.frame_1.isNullOrEmpty()) {
            val frame1s = commonCategoryRep.getByType(listOf(MasterDataType.KHUNG_1))
            for (frame in frame1s) {
                request.frame_1 = frame.value
                val summaryByFrame = getPlanSummary(
                    request,
                    dataExportMapping.filter { x -> x.frame_1 == frame.value }.toMutableList()
                )
                if (!summaryByFrame.data.isNullOrEmpty()) {
                    dataSummary.add(Pair(frame.value ?: "", summaryByFrame.data))
                }
            }
            request.frame_1 = null
            val planSummary = getPlanSummary(request, dataExportMapping.toMutableList())
            dataSummary.add(Pair("TOTAL", planSummary.data))
        } else {
            val planSummary = getPlanSummary(request, dataExportMapping.toMutableList())
            dataSummary.add(Pair(request.frame_1!!, planSummary.data))
        }

        rowNumber += 5
        val summaryHeaderRow = ExcelHelper.createRow(sheet, rowNumber, 800)

        ExcelHelper.setCellValue(summaryHeaderRow, 9, headerStyle, "Line")
        ExcelHelper.setCellValue(summaryHeaderRow, 10, headerStyle, "Công đoạn")
        ExcelHelper.setCellValue(summaryHeaderRow, 11, headerStyle, "")
        ExcelHelper.setCellValue(summaryHeaderRow, 12, headerStyle, "日程")
        ExcelHelper.setCellValue(summaryHeaderRow, 13, headerStyle, "Tổng")

        var summaryHeaderCol = 14
        for (col in columns) {
            ExcelHelper.setCellValueWithCalendar(workbook, summaryHeaderRow, summaryHeaderCol, headerStyle, col.value, col.isHoliday)
            summaryHeaderCol++
        }
        rowNumber++

        val styleCollections = mutableListOf<CellStyleModel>()
        for (summary in dataSummary) {
            for ((index, data) in summary.second!!.withIndex()) {
                if (index == 0) {
                    generateExcelColProcessInPlanSummary(workbook, sheet, rowNumber, styleCommon, data, styleCollections, PlanActiveTab.PLAN)

                    var rowIndex = rowNumber
                    for (detail in data.details!!) {
                        var dataRow = ExcelHelper.createRow(sheet, rowIndex)
                        ExcelHelper.setCellValue(dataRow, 11, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle, detail.type)
                        rowIndex++

                        for (i in 1 until 5) {
                            dataRow = ExcelHelper.createRow(sheet, rowIndex)
                            val st = if (i == 4) {
                                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                            } else {
                                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
                            }
                            ExcelHelper.setCellValue(dataRow, 11, st, "")
                            rowIndex++
                        }

                        rowIndex = rowNumber
                        for ((i, item) in detail.planSummaryData!!.withIndex()) {
                            dataRow = ExcelHelper.createRow(sheet, rowIndex)

                            val frame1ColStyle = if (i == 0) styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle
                            else if (i == detail.planSummaryData!!.size - 1) styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                            else styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle

                            ExcelHelper.setCellValue(dataRow, 9, frame1ColStyle, summary.first)

                            val titleStyle = if (item.titleKey == PlanTitle.PLAN_KEY) titlePlanStyle
                            else if (item.titleKey == PlanTitle.ACTUAL_KEY) titleActualStyle
                            else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) titleAccumulationStyle
                            else styleCommon

                            ExcelHelper.setCellValue(dataRow, 12, titleStyle, item.title)

                            val totalStyle = if (item.titleKey == PlanTitle.PLAN_KEY) planRowStyle
                            else if (item.titleKey == PlanTitle.ACTUAL_KEY) actualRowStyle
                            else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) accumulationRowStyle
                            else numberStyleCommon
                            ExcelHelper.setCellValue(dataRow, 13, totalStyle, NumberHelper.formatNumber(item.total?.toInt()))

                            var colIndex = 14
                            for (col in columns) {
                                val value = item.quantityByCalendars?.find { x -> x.key == col.key }?.value
                                val st = if (col.isHoliday) {
                                    if ((value?.toIntOrNull() ?: 0) < 0) {
                                        holidayWithNegativeNumStyle
                                    } else {
                                        if (item.titleKey == PlanTitle.PLAN_KEY) holidayStartRowStyle
                                        else if (item.titleKey == PlanTitle.ACTUAL_KEY) holidayActualRowStyle
                                        else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) holidayAccumulationRowStyle
                                        else holidayStyle
                                    }
                                } else {
                                    if ((value?.toIntOrNull() ?: 0) < 0) {
                                        negativeNumStyle
                                    } else {
                                        if (item.titleKey == PlanTitle.PLAN_KEY) planRowStyle
                                        else if (item.titleKey == PlanTitle.ACTUAL_KEY) actualRowStyle
                                        else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) accumulationRowStyle
                                        else numberStyleCommon
                                    }
                                }
                                ExcelHelper.setCellValue(dataRow, colIndex, st, NumberHelper.formatNumber(value?.toInt()))
                                colIndex++
                            }
                            rowIndex++
                        }
                        rowNumber = rowIndex
                    }
                } else {
                    generateExcelColProcessInPlanSummary(sheet, rowNumber, data, styleCollections, PlanActiveTab.PLAN)

                    var rowIndex = rowNumber
                    for (detail in data.details!!) {
                        var dataRow = ExcelHelper.createRow(sheet, rowIndex)
                        ExcelHelper.setCellValue(dataRow, 11, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle, detail.type)
                        rowIndex++

                        for (i in 1 until 5) {
                            dataRow = ExcelHelper.createRow(sheet, rowIndex)
                            val st = if (i == 4) {
                                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                            } else {
                                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
                            }
                            ExcelHelper.setCellValue(dataRow, 11, st, "")
                            rowIndex++
                        }

                        rowIndex = rowNumber
                        for ((i, item) in detail.planSummaryData!!.withIndex()) {
                            dataRow = ExcelHelper.createRow(sheet, rowIndex)

                            val frame1ColStyle = if (i == 0) styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle
                            else if (i == detail.planSummaryData!!.size - 1) styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                            else styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle

                            ExcelHelper.setCellValue(dataRow, 9, frame1ColStyle, summary.first)

                            val titleStyle = if (item.titleKey == PlanTitle.PLAN_KEY) titlePlanStyle
                            else if (item.titleKey == PlanTitle.ACTUAL_KEY) titleActualStyle
                            else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) titleAccumulationStyle
                            else styleCommon

                            ExcelHelper.setCellValue(dataRow, 12, titleStyle, item.title)

                            val totalStyle = if (item.titleKey == PlanTitle.PLAN_KEY) planRowStyle
                            else if (item.titleKey == PlanTitle.ACTUAL_KEY) actualRowStyle
                            else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) accumulationRowStyle
                            else numberStyleCommon
                            ExcelHelper.setCellValue(dataRow, 13, totalStyle, NumberHelper.formatNumber(item.total?.toInt()))

                            var colIndex = 14
                            for (col in columns) {
                                val value = item.quantityByCalendars?.find { x -> x.key == col.key }?.value
                                val st = if (col.isHoliday) {
                                    if ((value?.toIntOrNull() ?: 0) < 0) {
                                        holidayWithNegativeNumStyle
                                    } else {
                                        if (item.titleKey == PlanTitle.PLAN_KEY) holidayStartRowStyle
                                        else if (item.titleKey == PlanTitle.ACTUAL_KEY) holidayActualRowStyle
                                        else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) holidayAccumulationRowStyle
                                        else holidayStyle
                                    }
                                } else {
                                    if ((value?.toIntOrNull() ?: 0) < 0) {
                                        negativeNumStyle
                                    } else {
                                        if (item.titleKey == PlanTitle.PLAN_KEY) planRowStyle
                                        else if (item.titleKey == PlanTitle.ACTUAL_KEY) actualRowStyle
                                        else if (item.titleKey == PlanTitle.PLAN_ACCUMULATION_KEY || item.titleKey == PlanTitle.ACTUAL_ACCUMULATION_KEY) accumulationRowStyle
                                        else numberStyleCommon
                                    }
                                }
                                ExcelHelper.setCellValue(dataRow, colIndex, st, NumberHelper.formatNumber(value?.toInt()))
                                colIndex++
                            }
                            rowIndex++
                        }
                        rowNumber = rowIndex
                    }
                }
            }
            val endRow = sheet.createRow(rowNumber)
            ExcelHelper.setCellValue(endRow, 9, styleCommon, "")
            rowNumber++
        }
    }

    private fun generateDataSheetSummary(
        workbook: Workbook,
        columns: List<CalendarResponse>,
        dataSummary: List<Pair<String, List<PlanSummaryModel>?>>,
        headerStyle: CellStyle,
        frame_1: String?
    ) {
        val sheet = workbook.getSheetAt(0)
        val sheetName = if (frame_1.isNullOrEmpty()) sheet.sheetName else "${sheet.sheetName} - $frame_1"
        workbook.setSheetName(workbook.getSheetIndex(sheet), sheetName)

        val headerRow = ExcelHelper.createRow(sheet, 0, 800)

        ExcelHelper.setCellValue(headerRow, 0, headerStyle, "Line")
        sheet.setColumnWidth(0, 2500)

        ExcelHelper.setCellValue(headerRow, 1, headerStyle, "Công đoạn")
        sheet.setColumnWidth(1, 7000)

        ExcelHelper.setCellValue(headerRow, 2, headerStyle, "")
        sheet.setColumnWidth(2, 5000)

        ExcelHelper.setCellValue(headerRow, 3, headerStyle, "日程")
        sheet.setColumnWidth(3, 2000)

        ExcelHelper.setCellValue(headerRow, 4, headerStyle, "Tổng")
        sheet.setColumnWidth(4, 3000)

        var headerCol = 5
        for (col in columns) {
            ExcelHelper.setCellValueWithCalendar(workbook, headerRow, headerCol, headerStyle, col.value, col.isHoliday)
            headerCol++
        }

        sheet.createFreezePane(0, 1)

        val styleCollections = mutableListOf<CellStyleModel>()
        //setting style
        val styleCommon = ExcelHelper.getCellStyleCommon(workbook)
        styleCommon.alignment = HorizontalAlignment.CENTER
        val fontTemplate = workbook.getFontAt(styleCommon.fontIndex)

        val planRowStyle = workbook.createCellStyle()
        planRowStyle.cloneStyleFrom(styleCommon)
        planRowStyle.borderBottom = BorderStyle.DOTTED
        planRowStyle.alignment = HorizontalAlignment.RIGHT

        val titlePlanStyle = workbook.createCellStyle()
        titlePlanStyle.cloneStyleFrom(styleCommon)
        titlePlanStyle.borderBottom = BorderStyle.DOTTED

        val redFont = workbook.createFont()
        redFont.fontName = fontTemplate.fontName
        redFont.fontHeightInPoints = fontTemplate.fontHeightInPoints
        redFont.color = IndexedColors.RED.index

        val actualRowStyle = workbook.createCellStyle()
        actualRowStyle.cloneStyleFrom(styleCommon)
        actualRowStyle.borderTop = BorderStyle.DOTTED
        actualRowStyle.alignment = HorizontalAlignment.RIGHT
        actualRowStyle.setFont(redFont)

        val titleActualStyle = workbook.createCellStyle()
        titleActualStyle.cloneStyleFrom(styleCommon)
        titleActualStyle.borderTop = BorderStyle.DOTTED
        titleActualStyle.setFont(redFont)

        val holidayStyle = workbook.createCellStyle()
        holidayStyle.cloneStyleFrom(styleCommon)
        holidayStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        holidayStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        holidayStyle.alignment = HorizontalAlignment.RIGHT

        val holidayStartRowStyle = workbook.createCellStyle()
        holidayStartRowStyle.cloneStyleFrom(styleCommon)
        holidayStartRowStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        holidayStartRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        holidayStartRowStyle.alignment = HorizontalAlignment.RIGHT
        holidayStartRowStyle.borderBottom = BorderStyle.DOTTED

        val holidayActualRowStyle = workbook.createCellStyle()
        holidayActualRowStyle.cloneStyleFrom(styleCommon)
        holidayActualRowStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        holidayActualRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
        holidayActualRowStyle.alignment = HorizontalAlignment.RIGHT
        holidayActualRowStyle.borderTop = BorderStyle.DOTTED
        holidayActualRowStyle.setFont(redFont)

        val numberStyleCommon = workbook.createCellStyle()
        numberStyleCommon.cloneStyleFrom(styleCommon)
        numberStyleCommon.alignment = HorizontalAlignment.RIGHT
        //

        var rowNumber = 1
        for (summary in dataSummary) {
            for ((index, data) in summary.second!!.withIndex()) {
                if (index == 0) {
                    generateExcelColProcessInPlanSummary(workbook, sheet, rowNumber, styleCommon, data, styleCollections)

                    var rowIndex = rowNumber
                    for ((iDetail, detail) in data.details!!.withIndex()) {
                        var dataRow = ExcelHelper.createRow(sheet, rowIndex)
                        ExcelHelper.setCellValue(dataRow, 2, styleCollections.find { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }?.cellStyle!!, detail.type)
                        rowIndex++

                        dataRow = ExcelHelper.createRow(sheet, rowIndex)
                        ExcelHelper.setCellValue(dataRow, 2, styleCollections.find { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }?.cellStyle!!, "")

                        rowIndex = rowNumber
                        for ((i, item) in detail.planSummaryData!!.withIndex()) {
                            dataRow = ExcelHelper.createRow(sheet, rowIndex)
                            val colLineStyle = if (i == 0 && iDetail == 0) {
                                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle
                            } else if (i == detail.planSummaryData!!.size - 1 && iDetail == data.details!!.size - 1) {
                                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                            } else {
                                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
                            }
                            ExcelHelper.setCellValue(dataRow, 0, colLineStyle, summary.first)

                            val titleStyle = if (item.titleKey == PlanTitle.PLAN_KEY) titlePlanStyle
                            else if (item.titleKey == PlanTitle.ACTUAL_KEY) titleActualStyle
                            else styleCommon

                            ExcelHelper.setCellValue(dataRow, 3, titleStyle, item.title)

                            val totalStyle = if (item.titleKey == PlanTitle.PLAN_KEY) planRowStyle
                            else if (item.titleKey == PlanTitle.ACTUAL_KEY) actualRowStyle
                            else numberStyleCommon

                            ExcelHelper.setCellValue(dataRow, 4, totalStyle, NumberHelper.formatNumber(item.total?.toInt()))

                            var colIndex = 5
                            for (col in columns) {
                                val value = item.quantityByCalendars?.find { x -> x.key == col.key }?.value
                                val st = if (col.isHoliday) {
                                    if (item.titleKey == PlanTitle.PLAN_KEY) holidayStartRowStyle
                                    else if (item.titleKey == PlanTitle.ACTUAL_KEY) holidayActualRowStyle
                                    else holidayStyle
                                } else {
                                    if (item.titleKey == PlanTitle.PLAN_KEY) planRowStyle
                                    else if (item.titleKey == PlanTitle.ACTUAL_KEY) actualRowStyle
                                    else numberStyleCommon
                                }
                                ExcelHelper.setCellValue(dataRow, colIndex, st, NumberHelper.formatNumber(value?.toIntOrNull()))
                                colIndex++
                            }
                            rowIndex++
                        }
                        rowNumber = rowIndex
                    }
                } else {
                    generateExcelColProcessInPlanSummary(sheet, rowNumber, data, styleCollections)

                    var rowIndex = rowNumber
                    for ((iDetail, detail) in data.details!!.withIndex()) {
                        var dataRow = ExcelHelper.createRow(sheet, rowIndex)
                        ExcelHelper.setCellValue(dataRow, 2, styleCollections.find { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }?.cellStyle!!, detail.type)
                        rowIndex++

                        dataRow = ExcelHelper.createRow(sheet, rowIndex)
                        ExcelHelper.setCellValue(dataRow, 2, styleCollections.find { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }?.cellStyle!!, "")

                        rowIndex = rowNumber
                        for ((i, item) in detail.planSummaryData!!.withIndex()) {
                            dataRow = ExcelHelper.createRow(sheet, rowIndex)
                            val colLineStyle = if (i == 0 && iDetail == 0) {
                                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle
                            } else if (i == detail.planSummaryData!!.size - 1 && iDetail == data.details!!.size - 1) {
                                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                            } else {
                                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
                            }
                            ExcelHelper.setCellValue(dataRow, 0, colLineStyle, summary.first)

                            val titleStyle = if (item.titleKey == PlanTitle.PLAN_KEY) titlePlanStyle
                            else if (item.titleKey == PlanTitle.ACTUAL_KEY) titleActualStyle
                            else styleCommon

                            ExcelHelper.setCellValue(dataRow, 3, titleStyle, item.title)

                            val totalStyle = if (item.titleKey == PlanTitle.PLAN_KEY) planRowStyle
                            else if (item.titleKey == PlanTitle.ACTUAL_KEY) actualRowStyle
                            else numberStyleCommon

                            ExcelHelper.setCellValue(dataRow, 4, totalStyle, NumberHelper.formatNumber(item.total?.toInt()))

                            var colIndex = 5
                            for (col in columns) {
                                val value = item.quantityByCalendars?.find { x -> x.key == col.key }?.value
                                val st = if (col.isHoliday) {
                                    if (item.titleKey == PlanTitle.PLAN_KEY) holidayStartRowStyle
                                    else if (item.titleKey == PlanTitle.ACTUAL_KEY) holidayActualRowStyle
                                    else holidayStyle
                                } else {
                                    if (item.titleKey == PlanTitle.PLAN_KEY) planRowStyle
                                    else if (item.titleKey == PlanTitle.ACTUAL_KEY) actualRowStyle
                                    else numberStyleCommon
                                }
                                ExcelHelper.setCellValue(dataRow, colIndex, st, NumberHelper.formatNumber(value?.toIntOrNull()))
                                colIndex++
                            }
                            rowIndex++
                        }
                        rowNumber = rowIndex
                    }
                }
            }
            val endRow = sheet.createRow(rowNumber)
            ExcelHelper.setCellValue(endRow, 0, styleCommon, "")
            rowNumber++
        }
    }

    private fun generateDataSheetEquipment(
        workbook: Workbook,
        columns: List<CalendarResponse>,
        dataEquipment: List<EquipmentProductivityModel>?,
        headerStyle: CellStyle
    ) {
        if (dataEquipment != null) {
            val sheet = workbook.getSheetAt(0)
            val headerRow = ExcelHelper.createRow(sheet, 0, 800)

            ExcelHelper.setCellValue(headerRow, 0, headerStyle, "Line")
            sheet.setColumnWidth(0, 2500)

            ExcelHelper.setCellValue(headerRow, 1, headerStyle, "Công đoạn")
            sheet.setColumnWidth(1, 6000)

            ExcelHelper.setCellValue(headerRow, 2, headerStyle, "")
            sheet.setColumnWidth(2, 3000)

            ExcelHelper.setCellValue(headerRow, 3, headerStyle, "Tổng")
            sheet.setColumnWidth(3, 4000)

            ExcelHelper.setCellValue(headerRow, 4, headerStyle, "Loại")
            sheet.setColumnWidth(4, 5000)

            var headerCol = 5
            for (col in columns) {
                ExcelHelper.setCellValueWithCalendar(workbook, headerRow, headerCol, headerStyle, col.value, col.isHoliday)
                headerCol++
            }

            val styleCollections = mutableListOf<CellStyleModel>()
            val style = ExcelHelper.getCellStyleCommon(workbook)

            val titleFirstRowStyle = workbook.createCellStyle()
            titleFirstRowStyle.cloneStyleFrom(style)
            titleFirstRowStyle.borderBottom = BorderStyle.DOTTED
            titleFirstRowStyle.alignment = HorizontalAlignment.LEFT

            val titleMiddleRowStyle = workbook.createCellStyle()
            titleMiddleRowStyle.cloneStyleFrom(style)
            titleMiddleRowStyle.borderBottom = BorderStyle.DOTTED
            titleMiddleRowStyle.borderTop = BorderStyle.DOTTED
            titleMiddleRowStyle.alignment = HorizontalAlignment.LEFT

            val titleEndRowStyle = workbook.createCellStyle()
            titleEndRowStyle.cloneStyleFrom(style)
            titleEndRowStyle.borderTop = BorderStyle.DOTTED
            titleEndRowStyle.alignment = HorizontalAlignment.LEFT

            val detailFirstRowStyle = workbook.createCellStyle()
            detailFirstRowStyle.cloneStyleFrom(style)
            detailFirstRowStyle.borderBottom = BorderStyle.DOTTED
            detailFirstRowStyle.alignment = HorizontalAlignment.RIGHT

            val detailMiddleRowStyle = workbook.createCellStyle()
            detailMiddleRowStyle.cloneStyleFrom(style)
            detailMiddleRowStyle.borderBottom = BorderStyle.DOTTED
            detailMiddleRowStyle.borderTop = BorderStyle.DOTTED
            detailMiddleRowStyle.alignment = HorizontalAlignment.RIGHT

            val detailEndRowStyle = workbook.createCellStyle()
            detailEndRowStyle.cloneStyleFrom(style)
            detailEndRowStyle.borderTop = BorderStyle.DOTTED
            detailEndRowStyle.alignment = HorizontalAlignment.RIGHT

            val holidayFirstRowStyle = workbook.createCellStyle()
            holidayFirstRowStyle.cloneStyleFrom(style)
            holidayFirstRowStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            holidayFirstRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            holidayFirstRowStyle.borderBottom = BorderStyle.DOTTED
            holidayFirstRowStyle.alignment = HorizontalAlignment.RIGHT

            val holidayMiddleRowStyle = workbook.createCellStyle()
            holidayMiddleRowStyle.cloneStyleFrom(style)
            holidayMiddleRowStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            holidayMiddleRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            holidayMiddleRowStyle.borderBottom = BorderStyle.DOTTED
            holidayMiddleRowStyle.borderTop = BorderStyle.DOTTED
            holidayMiddleRowStyle.alignment = HorizontalAlignment.RIGHT

            val holidayEndRowStyle = workbook.createCellStyle()
            holidayEndRowStyle.cloneStyleFrom(style)
            holidayEndRowStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
            holidayEndRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            holidayEndRowStyle.borderTop = BorderStyle.DOTTED
            holidayEndRowStyle.alignment = HorizontalAlignment.RIGHT

            val orangeStyle = workbook.createCellStyle()
            orangeStyle.cloneStyleFrom(style)
            orangeStyle.fillForegroundColor = IndexedColors.LIGHT_ORANGE.index
            orangeStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            orangeStyle.borderTop = BorderStyle.DOTTED
            orangeStyle.alignment = HorizontalAlignment.RIGHT

            val yellowStyle = workbook.createCellStyle()
            yellowStyle.cloneStyleFrom(style)
            yellowStyle.fillForegroundColor = IndexedColors.LIGHT_YELLOW.index
            yellowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND
            yellowStyle.borderTop = BorderStyle.DOTTED
            yellowStyle.alignment = HorizontalAlignment.RIGHT

            var rowNumber = 1
            var index = 0
            val groupedByFrame1: Map<String?, List<EquipmentProductivityModel>> = dataEquipment.groupBy { it.frame1 }
            groupedByFrame1.forEach { (frame1Value, frame1List) ->
                for (planProcess in frame1List) {
                    val rowProcessIndex = if (index == 0) {
                        generateExcelRowProcessEquipment(workbook, sheet, rowNumber, style, planProcess, styleCollections)
                    } else {
                        generateExcelRowProcessEquipment(sheet, rowNumber, planProcess, styleCollections)
                    }
                    var rowTitleIndex = rowNumber
                    for ((iItem, item) in planProcess.processDetail!!.withIndex()) {
                        var dataRow = ExcelHelper.createRow(sheet, rowTitleIndex)
                        ExcelHelper.setCellValue(dataRow, 2, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle, item.name)
                        ExcelHelper.setCellValue(dataRow, 3, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle, (item.totalProcess ?: 0).toString())

                        rowTitleIndex++
                        dataRow = ExcelHelper.createRow(sheet, rowTitleIndex)
                        ExcelHelper.setCellValue(dataRow, 2, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle, "")
                        ExcelHelper.setCellValue(dataRow, 3, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle, "")

                        rowTitleIndex++
                        dataRow = ExcelHelper.createRow(sheet, rowTitleIndex)
                        ExcelHelper.setCellValue(dataRow, 2, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle, "")
                        ExcelHelper.setCellValue(dataRow, 3, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle, "")

                        rowTitleIndex++
                        dataRow = ExcelHelper.createRow(sheet, rowTitleIndex)
                        ExcelHelper.setCellValue(dataRow, 2, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle, "")
                        ExcelHelper.setCellValue(dataRow, 3, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle, "")

                        var rowDataIndex = rowNumber
                        for ((iData, data) in item.processDetailList.withIndex()) {
                            dataRow = ExcelHelper.createRow(sheet, rowDataIndex)

                            val frame1ColStyle = if (iItem == 0 && iData == 0) styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle
                            else if (iItem == planProcess.processDetail!!.size - 1 && iData == item.processDetailList.size - 1) styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                            else styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle

                            ExcelHelper.setCellValue(dataRow, 0, frame1ColStyle, frame1Value)

                            val titleStyle = if (iData == 0) titleFirstRowStyle
                            else if (iData == item.processDetailList.size - 1) titleEndRowStyle
                            else titleMiddleRowStyle

                            ExcelHelper.setCellValue(dataRow, 4, titleStyle, data.type)

                            if (data.type == ProcessPlan.QUANTITY_MACHINE) {
                                dataRow.height = 700
                            }

                            var colIndex = 5
                            for (col in columns) {
                                val quantityByCalendar = data.quantityByCalendars.find { x -> x.key == col.key }
                                val st = if (data.type == ProcessPlan.QUANTITY_MACHINE) {
                                    quantityByCalendar?.value += '%'
                                    when (quantityByCalendar?.sort) {
                                        Color.YELLOW.toBigDecimalOrNull() -> yellowStyle
                                        Color.ORANGE.toBigDecimalOrNull() -> orangeStyle
                                        else -> if (col.isHoliday) holidayEndRowStyle else detailEndRowStyle
                                    }
                                } else {
                                    quantityByCalendar?.value = NumberHelper.formatNumber(quantityByCalendar?.value?.toIntOrNull())
                                    if (col.isHoliday) {
                                        if (iData == 0) holidayFirstRowStyle
                                        else if (iData == item.processDetailList.size - 1) holidayEndRowStyle
                                        else holidayMiddleRowStyle
                                    } else {
                                        if (iData == 0) detailFirstRowStyle
                                        else if (iData == item.processDetailList.size - 1) detailEndRowStyle
                                        else detailMiddleRowStyle
                                    }
                                }

                                ExcelHelper.setCellValue(dataRow, colIndex, st, quantityByCalendar?.value)
                                colIndex++
                            }
                            rowDataIndex++
                        }
                        rowTitleIndex++
                        rowNumber = rowTitleIndex
                    }

                    rowNumber = rowProcessIndex
                    index++
                }

                val endRow = sheet.createRow(rowNumber)
                ExcelHelper.setCellValue(endRow, 0, style, "")
                rowNumber++
            }
        }
    }

    //endregion

    //region PLAN_TEMP

    fun approve(request: PlanSearchRequest): BaseResponse<Boolean> {
        val planTemp = planRep.getPlanTemp() ?: throw BusinessException(CommonUtils.getMessage("validate.plan.notPlanApprove"))

        val planProductTemps = planProductRep.getPlanProductTemp()
        val planProcessTemps = planProcessRep.getPlanProcessTemp()
        val planDetailTemps = planDetailRep.getPlanDetailTemp()

        val planExist = planRep.getByMonth(planTemp.month!!, planTemp.year!!)
        if (planExist != null) {
            planTemp.version = (planExist.version ?: 0) + 1
            planRep.inActive(planExist.id!!)
        }
        planHistoryService.addFile(exportExcel(request), request.fileName)
        planRep.createPlan(planTemp, planProductTemps, planProcessTemps, planDetailTemps)

        return BaseResponse(true, CommonUtils.getMessage("action.succeeded"))
    }

    fun checkTemp(): BaseResponse<Boolean> {
        val planTemp = planRep.getPlanTemp()
        return BaseResponse(planTemp != null)
    }
    //endregion
}
