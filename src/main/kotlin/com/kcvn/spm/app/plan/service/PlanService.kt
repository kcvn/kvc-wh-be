package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.PlanDataByProcessModel
import com.kcvn.spm.app.plan.payload.model.PlanExportExcelModel
import com.kcvn.spm.app.plan.payload.model.PlanSummaryDetailModel
import com.kcvn.spm.app.plan.payload.model.PlanSummaryModel
import com.kcvn.spm.app.plan.payload.model.ProcessChildrenModel
import com.kcvn.spm.app.plan.payload.model.ProductPlanDetailModel
import com.kcvn.spm.app.plan.payload.model.ProductPlanModel
import com.kcvn.spm.app.plan.payload.request.PlanDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.PlanSummaryResponse
import com.kcvn.spm.app.plan.payload.response.ProductPlanDetailResponse
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.KeyAppSetting
import com.kcvn.spm.common.constants.MasterDataType
import com.kcvn.spm.common.constants.Mold
import com.kcvn.spm.common.constants.PlanProcessSummary
import com.kcvn.spm.common.constants.PlanStyleKey
import com.kcvn.spm.common.constants.PlanTitle
import com.kcvn.spm.common.constants.ProcessConvertCode
import com.kcvn.spm.common.constants.ProcessStatisticCode
import com.kcvn.spm.common.constants.ProcessUnit
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.CalendarResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.CellStyleModel
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.PlanProduct
import com.kcvn.spm.repository.AppSettingRepository
import com.kcvn.spm.repository.CommonCategoryRepository
import com.kcvn.spm.repository.HolidaysCalenderRepository
import com.kcvn.spm.repository.PlanDetailRepository
import com.kcvn.spm.repository.PlanProcessRepository
import com.kcvn.spm.repository.PlanProductRepository
import com.kcvn.spm.repository.ProcessGroupRepository
import com.kcvn.spm.repository.WorkResultRepository
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.FillPatternType
import org.apache.poi.ss.usermodel.HorizontalAlignment
import org.apache.poi.ss.usermodel.IndexedColors
import org.apache.poi.ss.usermodel.Sheet
import org.apache.poi.ss.usermodel.Workbook
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class PlanService(
    private val planProductRep: PlanProductRepository,
    private val planProcessRep: PlanProcessRepository,
    private val planDetailRep: PlanDetailRepository,
    private val workResultRep: WorkResultRepository,
    private val holidaysCalenderRep: HolidaysCalenderRepository,
    private val processGroupRep: ProcessGroupRepository,
    private val appSettingRep: AppSettingRepository,
    private val commonCategoryRep: CommonCategoryRepository
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

        if (request.planProductId.isEmpty()) throw BusinessException(CommonUtils.getMessage("plan.invalidParam"))
        if (request.startDate == null || request.endDate == null) throw BusinessException(CommonUtils.getMessage("plan.invalidTime"))

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        response.columns = DateTimeHelper.toCalendarColumn(
            DateTimeHelper.toTimeZone7(request.startDate)!!,
            DateTimeHelper.toTimeZone7(request.endDate)!!,
            holidayCalenders
        )

        val planProduct = planProductRep.getById(request.planProductId)
            ?: throw BusinessException(CommonUtils.getMessage("data.notExist"))

        val planProcesses = planProcessRep.getListPlanProcess(request.planProductId)
        val parentPlanProcess = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }
        val childrenPlanProcess = planProcesses.filter { x -> x.parentId != null }

        val planProcessIds = parentPlanProcess.mapNotNull { x -> x.id }
        val planDetails = planDetailRep.getPlanDetail(planProcessIds)

        val workResults = workResultRep.getForPlan(request.startDate!!, request.endDate!!, listOf(planProduct.productName ?: ""))

        response.data = parentPlanProcess.map { x ->
            val productPlan = ProductPlanDetailModel(
                frame_1 = planProduct.frame_1,
                mold = planProduct.mold,
                layerCode = x.layerCode,
                processCode = x.processCode,
                processName = x.processName,
                processNameJp = x.processNameJp,
                completionRate = x.completionRate,
                processConvertCode = x.processConvertCode,
                processSequence = x.processSequence,
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
            val planDetail = planDetailByProcess.filter { m -> m.title == PlanTitle.PLAN_KEY }.map { m ->
                KeyValueResponse(
                    DateTimeHelper.toString(m.planDate!!, DateTimeFormat.yyyyMMdd),
                    if (x.unit == ProcessUnit.BLOCK) m.blockQuantity?.toString() else m.sheetQuantity?.toString()
                )
            }
            val planAccumulations = planDetailByProcess.filter { m -> m.title == PlanTitle.PLAN_ACCUMULATION_KEY }.map { m ->
                KeyValueResponse(
                    DateTimeHelper.toString(m.planDate!!, DateTimeFormat.yyyyMMdd),
                    if (x.unit == ProcessUnit.BLOCK) m.blockQuantity?.toString() else m.sheetQuantity?.toString()
                )
            }.sortedBy { m -> m.key }

            val workResultData = workResults.filter { m -> m.processCode == x.processCode && m.layerCode == x.layerCode }
                .groupBy { m -> Triple(m.processCode, m.layerCode, DateTimeHelper.toString(m.summaryResultDate!!, DateTimeFormat.yyyyMMdd)) }.map { m ->
                    KeyValueResponse(
                        m.key.third,
                        if (x.unit == ProcessUnit.BLOCK) m.value.sumOf { t -> t.goodTapeQuantity ?: 0 }.toString() else m.value.sumOf { t -> t.goodSheetQuantity ?: 0 }.toString()
                    )
                }.sortedBy { m -> m.key }
            val workResultAccumulations = calculateAccumulation(workResultData, productPlan.sumInventory)

            val planData = mutableListOf<PlanDataByProcessModel>()
            planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN, titleKey = PlanTitle.PLAN_KEY, quantityByCalendars = planDetail))
            planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN_ACCUMULATION, titleKey = PlanTitle.PLAN_ACCUMULATION_KEY, quantityByCalendars = planAccumulations))
            planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL, titleKey = PlanTitle.ACTUAL_KEY, quantityByCalendars = workResultData))
            planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL_ACCUMULATION, titleKey = PlanTitle.ACTUAL_ACCUMULATION_KEY, quantityByCalendars = workResultAccumulations))
            planData.add(PlanDataByProcessModel(title = PlanTitle.DIFFERENCE, titleKey = PlanTitle.DIFFERENCE_KEY, quantityByCalendars = calculateDifference(planAccumulations, workResultAccumulations)))

            productPlan.planData = planData
            productPlan
        }.sortedWith(compareBy<ProductPlanDetailModel> { x -> x.layerCode?.toInt() }.thenBy { x -> x.processSequence })
        return response
    }

    private fun getDataExportExcel(planProducts: List<PlanProduct>, colStartDate: OffsetDateTime, colEndDate: OffsetDateTime): List<PlanExportExcelModel> {
        val planProductIds = planProducts.mapNotNull { x -> x.id }
        val productNames = planProducts.mapNotNull { x -> x.productName }.distinct()

        val planProcesses = planProcessRep.getListPlanProcess(planProductIds)
        val parentPlanProcess = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }.sortedBy { x -> x.planProductId }
        val childrenPlanProcess = planProcesses.filter { x -> x.parentId != null }

        val planProcessIds = parentPlanProcess.mapNotNull { x -> x.id }
        val planDetails = planDetailRep.getPlanDetail(planProcessIds)

        val workResults = workResultRep.getForPlan(colStartDate, colEndDate, productNames)

        val data = mutableListOf<PlanExportExcelModel>()

        for (planProduct in planProducts) {
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
                        DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                        if (x.unit == ProcessUnit.BLOCK) t.blockQuantity?.toString() else t.sheetQuantity?.toString()
                    )
                }
                val planAccumulations = planDetailByProcess.filter { t -> t.title == PlanTitle.PLAN_ACCUMULATION_KEY }.map { t ->
                    KeyValueResponse(
                        DateTimeHelper.toString(t.planDate!!, DateTimeFormat.yyyyMMdd),
                        if (x.unit == ProcessUnit.BLOCK) t.blockQuantity?.toString() else t.sheetQuantity?.toString()
                    )
                }

                val workResultData = workResults.filter { m -> m.itemName == planProduct.productName && m.processCode == x.processCode && m.layerCode == x.layerCode }
                    .groupBy { m -> Triple(m.processCode, m.layerCode, DateTimeHelper.toString(m.summaryResultDate!!, DateTimeFormat.yyyyMMdd)) }.map { m ->
                        KeyValueResponse(
                            m.key.third,
                            if (x.unit == ProcessUnit.BLOCK) m.value.sumOf { t -> t.goodTapeQuantity ?: 0 }.toString() else m.value.sumOf { t -> t.goodSheetQuantity ?: 0 }.toString()
                        )
                    }.sortedBy { m -> m.key }
                val workResultAccumulations = calculateAccumulation(workResultData, productPlan.sumInventory)

                val planData = mutableListOf<PlanDataByProcessModel>()
                planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN, titleKey = PlanTitle.PLAN_KEY, quantityByCalendars = planDetail))
                planData.add(PlanDataByProcessModel(title = PlanTitle.PLAN_ACCUMULATION, titleKey = PlanTitle.PLAN_ACCUMULATION_KEY, quantityByCalendars = planAccumulations))
                planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL, titleKey = PlanTitle.ACTUAL_KEY, quantityByCalendars = workResultData))
                planData.add(PlanDataByProcessModel(title = PlanTitle.ACTUAL_ACCUMULATION, titleKey = PlanTitle.ACTUAL_ACCUMULATION_KEY, quantityByCalendars = workResultAccumulations))
                planData.add(PlanDataByProcessModel(title = PlanTitle.DIFFERENCE, titleKey = PlanTitle.DIFFERENCE_KEY, quantityByCalendars = calculateDifference(planAccumulations, workResultAccumulations)))

                productPlan.planData = planData
                productPlan
            }.sortedWith(compareBy<ProductPlanDetailModel> { x -> x.layerCode?.toInt() }.thenBy { x -> x.processSequence })
            data.add(productExport)
        }

        return data
    }

    private fun calculateAccumulation(data: List<KeyValueResponse>, firstValue: Int? = null): List<KeyValueResponse> {
        var value = firstValue ?: 0
        val response = mutableListOf<KeyValueResponse>()
        for (item in data) {
            value += (item.value?.toInt() ?: 0)
            response.add(KeyValueResponse(item.key, value.toString()))
        }
        return response
    }

    private fun calculateDifference(sourceData: List<KeyValueResponse>, compareData: List<KeyValueResponse>): List<KeyValueResponse> {
        val response = mutableListOf<KeyValueResponse>()
        for (item in compareData) {
            val sourceValue = sourceData.find { x -> x.key == item.key }
            val diffValue = (item.value?.toInt() ?: 0) - (sourceValue?.value?.toInt() ?: 0)
            response.add(KeyValueResponse(item.key, diffValue.toString()))
        }
        return response
    }

    //endregion

    //region PLAN SUMMARY

    fun getPlanSummary(request: PlanSearchRequest,  dataExports: MutableList<PlanExportExcelModel> = mutableListOf()): PlanSummaryResponse {
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
            dataExports.addAll(getDataExportExcel(planProducts, request.startDate!!, request.endDate!!))
        }
        val dataExportFlattens = dataExports.asSequence().mapNotNull { x -> x.productPlanDetails }.flatten().filter { x ->
            !x.processConvertCode.isNullOrEmpty()
                && (PlanProcessSummary.DATA.any { m -> m == x.processConvertCode } || x.processConvertCode!!.startsWith(ProcessConvertCode.M))
        }

        val dataSummary = dataExportFlattens.groupBy { x -> x.processConvertCode }.map { x ->
            val process = processGroups.find { m -> m.processStatisticCode == x.key }
            val summary = PlanSummaryModel(
                processName = process?.description,
                processNameJp = process?.descriptionJp,
                processConvertCode = x.key,
                processSequence = process?.sortOrder?.toInt(),
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
        }.sortedBy { x -> x.processSequence }.toMutableList()

        val dataDucLo = dataSummary.filter { x -> x.processConvertCode == ProcessConvertCode.T || x.processConvertCode == ProcessConvertCode.TH }
        if (dataDucLo.isNotEmpty()) {
            val moldByFrame1s = (appSettingRep.findByKey("${KeyAppSetting.MOLD_BY_FRAME1}_${request.frame_1}")?.value?.split(",")
                ?: Mold.DATA_BY_FRAME1(request.frame_1)).filter { x -> request.mold.isNullOrEmpty() || x == request.mold }
            val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.T }
            val ducLo = PlanSummaryModel(
                processName = process?.description,
                processNameJp = process?.descriptionJp,
                processConvertCode = "${ProcessConvertCode.T}/${ProcessConvertCode.TH}",
                processSequence = process?.sortOrder?.toInt(),
                details = mutableListOf(
                    PlanSummaryDetailModel(
                        type = if (moldByFrame1s.size > 1) CommonUtils.getMessage("excel.rowTotal") else moldByFrame1s.first(),
                        planSummaryData = dataDucLo.asSequence().mapNotNull { x -> x.details }.flatten().mapNotNull { x -> x.planSummaryData }.flatten()
                            .groupBy { x -> Pair(x.titleKey, x.title) }.map { x ->
                                val data = PlanDataByProcessModel(title = x.key.second, titleKey = x.key.first)
                                data.quantityByCalendars = x.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }.toList()
                    )
                )
            )
            if (moldByFrame1s.size > 1) {
                for (mold in moldByFrame1s) {
                    var dataMold = dataExportFlattens.filter { x ->
                        x.mold == mold
                            && (x.processConvertCode == ProcessConvertCode.T || x.processConvertCode == ProcessConvertCode.TH)
                    }.groupBy { x -> x.mold }.mapNotNull { x ->
                        PlanSummaryDetailModel(
                            type = x.key,
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
                            type = mold,
                            planSummaryData = PlanTitle.DATA.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                        )
                    }
                    ducLo.details!!.add(dataMold)
                }
            }

            dataSummary.removeAll(dataDucLo)
            dataSummary.add(ducLo)
        }

        val dataInMach = dataSummary.filter { x -> x.processConvertCode == ProcessConvertCode.TAN || x.processConvertCode == ProcessConvertCode.ZEN }
        if (dataInMach.isNotEmpty()) {
            val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.IN_MACH }
            val inMach = PlanSummaryModel(
                processName = process?.description,
                processNameJp = process?.descriptionJp,
                processConvertCode = "${ProcessConvertCode.TAN}/${ProcessConvertCode.ZEN}",
                processSequence = process?.sortOrder?.toInt(),
                details = mutableListOf(
                    PlanSummaryDetailModel(
                        type = CommonUtils.getMessage("excel.rowTotal"),
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

            for (code in listOf(ProcessConvertCode.TAN, ProcessConvertCode.ZEN)) {
                var dataTanZen = dataExportFlattens.filter { x -> x.processConvertCode == code }.groupBy { x -> x.processConvertCode }.mapNotNull { x ->
                    PlanSummaryDetailModel(
                        type = x.key,
                        planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                            .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                                val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                                data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }
                    )
                }.firstOrNull()
                if (dataTanZen == null) {
                    dataTanZen = PlanSummaryDetailModel(
                        type = code,
                        planSummaryData = PlanTitle.DATA.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                    )
                }
                inMach.details!!.add(dataTanZen)
            }

            dataSummary.removeAll(dataInMach)
            dataSummary.add(inMach)
        }

        val dataInLo = dataSummary.filter { x -> x.processConvertCode == ProcessConvertCode.HP_ALL || x.processConvertCode == ProcessConvertCode.HP }
        if (dataInLo.isNotEmpty()) {
            val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.IN_LO }
            val inLo = PlanSummaryModel(
                processName = process?.description,
                processNameJp = process?.descriptionJp,
                processConvertCode = "${ProcessConvertCode.HP_ALL}/${ProcessConvertCode.HP}",
                processSequence = process?.sortOrder?.toInt(),
                details = mutableListOf(
                    PlanSummaryDetailModel(
                        type = CommonUtils.getMessage("excel.rowTotal"),
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

        val dataGhepLop = dataSummary.filter { x -> !x.processConvertCode.isNullOrEmpty() && x.processConvertCode!!.startsWith(ProcessConvertCode.M) }
        if (dataGhepLop.isNotEmpty()) {
            val process = processGroups.find { x -> x.processStatisticCode == ProcessStatisticCode.GHEP_LOP_SUM }
            val ghepLop = PlanSummaryModel(
                processName = process?.description,
                processNameJp = process?.descriptionJp,
                processConvertCode = "${ProcessConvertCode.M_ALL}/${ProcessConvertCode.M_ANY}",
                processSequence = process?.sortOrder?.toInt(),
                details = mutableListOf(
                    PlanSummaryDetailModel(
                        type = CommonUtils.getMessage("excel.rowTotal"),
                        planSummaryData = dataGhepLop.asSequence().mapNotNull { x -> x.details }.flatten().mapNotNull { x -> x.planSummaryData }.flatten()
                            .groupBy { x -> Pair(x.titleKey, x.title) }.map { x ->
                                val data = PlanDataByProcessModel(title = x.key.second, titleKey = x.key.first)
                                data.quantityByCalendars = x.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                    .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                                data
                            }.toList()
                    )
                )
            )

            var mAll = dataExportFlattens.filter { x -> x.processConvertCode == ProcessConvertCode.M_ALL }.groupBy { x -> x.processConvertCode }.mapNotNull { x ->
                PlanSummaryDetailModel(
                    type = x.key,
                    planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                        .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                            val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                            data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                            data
                        }
                )
            }.firstOrNull()
            if (mAll == null) {
                mAll = PlanSummaryDetailModel(
                    type = ProcessConvertCode.M_ALL,
                    planSummaryData = PlanTitle.DATA.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                )
            }
            ghepLop.details!!.add(mAll)

            var mTan = dataExportFlattens.filter { x ->
                !x.processConvertCode.isNullOrEmpty()
                    && x.processConvertCode != ProcessConvertCode.M_ALL
                    && x.processConvertCode!!.startsWith(ProcessConvertCode.M)
            }.map { x ->
                x.processConvertCode = ProcessConvertCode.M_ANY
                x
            }.groupBy { x -> x.processConvertCode }.mapNotNull { x ->
                PlanSummaryDetailModel(
                    type = "M2*3, M3*4...",
                    planSummaryData = x.value.mapNotNull { m -> m.planData }.flatten()
                        .groupBy { m -> Pair(m.titleKey, m.title) }.map { m ->
                            val data = PlanDataByProcessModel(title = m.key.second, titleKey = m.key.first)
                            data.quantityByCalendars = m.value.mapNotNull { t -> t.quantityByCalendars }.flatten()
                                .groupBy { t -> t.key }.map { t -> KeyValueResponse(t.key, t.value.sumOf { p -> (p.value?.toInt() ?: 0) }.toString()) }
                            data
                        }
                )
            }.firstOrNull()
            if (mTan == null) {
                mTan = PlanSummaryDetailModel(
                    type = ProcessConvertCode.M_ANY,
                    planSummaryData = PlanTitle.DATA.map { x -> PlanDataByProcessModel(title = x.value, titleKey = x.key, quantityByCalendars = listOf()) }
                )
            }
            ghepLop.details!!.add(mTan)

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
        styleCollections: MutableList<CellStyleModel>
    ): Int {
        var rowIndex = rowNumber
        var dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)

        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = data.processName,
            isBorderLeft = true, isBorderRight = true, isBorderTop = true, isBorderBottom = false,
            isBold = false, isAlignCenter = false
        )
        styleCollections.add(CellStyleModel(0, dataRow.getCell(0).cellStyle, PlanStyleKey.PLAN_SUMMARY_FIRST_ROW))

        rowIndex++
        dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = data.processNameJp,
            isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false,
            isBold = false, isAlignCenter = false
        )
        styleCollections.add(CellStyleModel(0, dataRow.getCell(0).cellStyle, PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW))

        rowIndex++
        dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValueCustom(
            workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = data.processConvertCode,
            isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = false,
            isBold = false, isAlignCenter = false
        )

        rowIndex++
        for (i in 3 until (data.details!!.size * 5)) {
            dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
            ExcelHelper.setCellValueCustom(
                workbook = workbook, row = dataRow, colIndex = 0, styleTemplate = style, value = "",
                isBorderLeft = true, isBorderRight = true, isBorderTop = false, isBorderBottom = (i == ((data.details!!.size * 5) - 1)),
                isBold = false, isAlignCenter = false
            )
            if (i == ((data.details!!.size * 5) - 1)) {
                styleCollections.add(CellStyleModel(0, dataRow.getCell(0).cellStyle, PlanStyleKey.PLAN_SUMMARY_END_ROW))
            }
            rowIndex++
        }
        return rowIndex
    }

    private fun generateExcelColProcessInPlanSummary(
        sheet: Sheet,
        rowNumber: Int,
        data: PlanSummaryModel,
        styleCollections: MutableList<CellStyleModel>
    ): Int {
        var rowIndex = rowNumber
        var dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValue(dataRow, 0, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle, data.processName)

        rowIndex++
        dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValue(dataRow, 0, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle, data.processNameJp)

        rowIndex++
        dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
        ExcelHelper.setCellValue(dataRow, 0, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle, data.processConvertCode)

        rowIndex++
        for (i in 3 until (data.details!!.size * 5)) {
            dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
            val st = if (i == ((data.details!!.size * 5) - 1)) {
                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
            } else {
                styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
            }
            ExcelHelper.setCellValue(dataRow, 0, st, "")
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

        val planProducts = planProductRep.getListPlanProduct(request)
        val dataExports = getDataExportExcel(planProducts, request.startDate!!, request.endDate!!)
        if (dataExports.isEmpty()) throw BusinessException(CommonUtils.getMessage("excel.export.noData"))

        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportPlanTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }

        generateDataSheetPlan(workbook, columns, planProducts, dataExports)
        val headerStyle = workbook.getSheetAt(0).getRow(0).getCell(0).cellStyle

        if (request.frame_1.isNullOrEmpty()) {
            val planSummary = getPlanSummary(request, dataExports.toMutableList())
            generateDataSheetSummary(workbook, columns, planSummary.data, "Tổng hợp", headerStyle)
            val frame1s = commonCategoryRep.getByType(listOf(MasterDataType.KHUNG_1))
            for (item in frame1s) {
                request.frame_1 = item.value
                val planSummaryByFrame = getPlanSummary(request, dataExports.filter { x -> x.frame_1 == item.value }.toMutableList())
                generateDataSheetSummary(workbook, columns, planSummaryByFrame.data, item.value ?: "", headerStyle)
            }
        }
        else{
            val planSummary = getPlanSummary(request, dataExports.toMutableList())
            generateDataSheetSummary(workbook, columns, planSummary.data, request.frame_1!!, headerStyle)
        }

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

    private fun generateDataSheetPlan(workbook: Workbook, columns: List<CalendarResponse>, planProducts: List<PlanProduct>, dataExports: List<PlanExportExcelModel>) {
        val sheet = workbook.getSheetAt(0)
        val headerRow = sheet.getRow(0)
        var headerCol = 12
        val headerStyle = headerRow.getCell(0).cellStyle
        for (col in columns) {
            ExcelHelper.setCellValueWithCalendar(workbook, headerRow, headerCol, headerStyle, col.value, col.isHoliday)
            headerCol++
        }

        val styleCommon = ExcelHelper.getCellStyleCommon(workbook)
        styleCommon.alignment = HorizontalAlignment.CENTER

        val firstRowStyle = workbook.createCellStyle()
        firstRowStyle.cloneStyleFrom(styleCommon)
        firstRowStyle.fillForegroundColor = IndexedColors.PALE_BLUE.index
        firstRowStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

        val holidayStyle = workbook.createCellStyle()
        holidayStyle.cloneStyleFrom(styleCommon)
        holidayStyle.fillForegroundColor = IndexedColors.GREY_25_PERCENT.index
        holidayStyle.fillPattern = FillPatternType.SOLID_FOREGROUND

        var rowNumber = 1
        for (planProduct in planProducts) {
            val dataExport = dataExports.find { x -> x.id == planProduct.id }
            if (dataExport == null || dataExport.productPlanDetails.isNullOrEmpty()) continue
            var isNextProduct = true
            for (planProcess in dataExport.productPlanDetails!!) {
                var rowProcessIndex = rowNumber
                val styleProcess = if (isNextProduct) firstRowStyle else styleCommon
                var dataRow = sheet.getRow(rowProcessIndex) ?: sheet.createRow(rowProcessIndex)

                ExcelHelper.setCellValue(dataRow, 5, styleProcess, planProcess.layerCode)
                ExcelHelper.setCellValue(dataRow, 6, styleProcess, planProcess.processCode)
                ExcelHelper.setCellValue(dataRow, 7, styleProcess, planProcess.processGroup)
                ExcelHelper.setCellValue(dataRow, 8, styleProcess, "${planProcess.completionRate?.toString()} %")
                ExcelHelper.setCellValue(dataRow, 9, styleProcess, planProcess.inventory?.toString())
                ExcelHelper.setCellValue(dataRow, 10, styleProcess, planProcess.sumInventory?.toString())

                rowProcessIndex++
                if (!planProcess.processChildren.isNullOrEmpty()) {
                    for (children in planProcess.processChildren!!) {
                        dataRow = sheet.getRow(rowProcessIndex) ?: sheet.createRow(rowProcessIndex)

                        ExcelHelper.setCellValue(dataRow, 5, styleCommon, children.layerCode)
                        ExcelHelper.setCellValue(dataRow, 6, styleCommon, children.processCode)
                        ExcelHelper.setCellValue(dataRow, 7, styleCommon, "")
                        ExcelHelper.setCellValue(dataRow, 8, styleCommon, "")
                        ExcelHelper.setCellValue(dataRow, 9, styleCommon, children.inventory?.toString())
                        ExcelHelper.setCellValue(dataRow, 10, styleCommon, "")

                        rowProcessIndex++
                    }
                }

                var rowDataIndex = rowNumber
                for (item in planProcess.planData!!) {
                    val style = if (isNextProduct) firstRowStyle else styleCommon
                    dataRow = sheet.getRow(rowDataIndex) ?: sheet.createRow(rowDataIndex)

                    ExcelHelper.setCellValue(dataRow, 0, style, dataExport.productName)
                    ExcelHelper.setCellValue(dataRow, 1, style, dataExport.pcsSh?.toString())
                    ExcelHelper.setCellValue(dataRow, 2, style, dataExport.blockSh?.toString())
                    ExcelHelper.setCellValue(dataRow, 3, style, dataExport.frame_1)
                    ExcelHelper.setCellValue(dataRow, 4, style, dataExport.mold)

                    ExcelHelper.setCellValue(dataRow, 11, style, "${planProcess.processConvertCode} ${item.title}")

                    var colIndex = 12
                    for (col in columns) {
                        val value = item.quantityByCalendars?.find { x -> x.key == col.key }?.value
                        ExcelHelper.setCellValue(dataRow, colIndex, (if (col.isHoliday) holidayStyle else style), value)
                        colIndex++
                    }

                    rowDataIndex++
                    isNextProduct = false
                }
                if (rowDataIndex > rowProcessIndex) {
                    for (i in rowProcessIndex until rowDataIndex) {
                        dataRow = sheet.getRow(i) ?: sheet.createRow(i)
                        ExcelHelper.setCellValue(dataRow, 5, styleCommon, planProcess.layerCode)
                        ExcelHelper.setCellValue(dataRow, 6, styleCommon, "")
                        ExcelHelper.setCellValue(dataRow, 7, styleCommon, "")
                        ExcelHelper.setCellValue(dataRow, 8, styleCommon, "")
                        ExcelHelper.setCellValue(dataRow, 9, styleCommon, "")
                        ExcelHelper.setCellValue(dataRow, 10, styleCommon, "")
                    }
                    rowNumber = rowDataIndex
                } else {
                    if (rowDataIndex == rowProcessIndex) rowNumber = rowDataIndex
                    else {
                        for (i in rowDataIndex until rowProcessIndex) {
                            dataRow = sheet.getRow(i) ?: sheet.createRow(i)
                            ExcelHelper.setCellValue(dataRow, 0, styleCommon, dataExport.productName)
                            ExcelHelper.setCellValue(dataRow, 1, styleCommon, dataExport.pcsSh?.toString())
                            ExcelHelper.setCellValue(dataRow, 2, styleCommon, dataExport.blockSh?.toString())
                            ExcelHelper.setCellValue(dataRow, 3, styleCommon, dataExport.frame_1)
                            ExcelHelper.setCellValue(dataRow, 4, styleCommon, dataExport.mold)
                            ExcelHelper.setCellValue(dataRow, 11, styleCommon, "")

                            var colIndex = 12
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
    }

    private fun generateDataSheetSummary(workbook: Workbook, columns: List<CalendarResponse>, dataSummary: List<PlanSummaryModel>?, sheetName: String, headerStyle: CellStyle) {
        val sheet = workbook.createSheet(sheetName)
        val headerRow = sheet.getRow(0) ?: sheet.createRow(0)
        headerRow.height = 800

        ExcelHelper.setCellValue(headerRow, 0, headerStyle, "")
        sheet.setColumnWidth(0, 6000)

        ExcelHelper.setCellValue(headerRow, 0, headerStyle, "")
        sheet.setColumnWidth(1, 5000)

        ExcelHelper.setCellValue(headerRow, 0, headerStyle, "")
        sheet.setColumnWidth(2, 2000)

        var headerCol = 3
        for (col in columns) {
            ExcelHelper.setCellValueWithCalendar(workbook, headerRow, headerCol, headerStyle, col.value, col.isHoliday)
            headerCol++
        }

        sheet.createFreezePane(0, 1)

        val styleCollections = mutableListOf<CellStyleModel>()
        val style = ExcelHelper.getCellStyleCommon(workbook)

        var rowNumber = 1
        for ((index, data) in dataSummary!!.withIndex()) {
            if (index == 0) {
                generateExcelColProcessInPlanSummary(workbook, sheet, rowNumber, style, data, styleCollections)

                var rowIndex = rowNumber
                for (detail in data.details!!) {
                    var dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                    ExcelHelper.setCellValue(dataRow, 1, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle, detail.type)
                    rowIndex++

                    for (i in 1 until 5) {
                        dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                        val st = if (i == 4) {
                            styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                        } else {
                            styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
                        }
                        ExcelHelper.setCellValue(dataRow, 1, st, "")
                        rowIndex++
                    }

                    rowIndex = rowNumber
                    for (item in detail.planSummaryData!!) {
                        dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                        ExcelHelper.setCellValue(dataRow, 2, style, item.title)

                        var colIndex = 3
                        for (col in columns) {
                            val value = item.quantityByCalendars?.find { x -> x.key == col.key }?.value
                            ExcelHelper.setCellValueWithCalendar(workbook, dataRow, colIndex, style, value, col.isHoliday)
                            colIndex++
                        }
                        if (rowIndex == rowNumber) {
                            styleCollections.addAll(dataRow.map { x -> CellStyleModel(x.columnIndex, x.cellStyle, PlanStyleKey.PLAN_SUMMARY_DETAIL) })
                        }
                        rowIndex++
                    }
                    rowNumber = rowIndex
                }
            }
            else {
                generateExcelColProcessInPlanSummary(sheet, rowNumber, data, styleCollections)

                var rowIndex = rowNumber
                for (detail in data.details!!) {
                    var dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                    ExcelHelper.setCellValue(dataRow, 1, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_FIRST_ROW }.cellStyle, detail.type)
                    rowIndex++

                    for (i in 1 until 5) {
                        dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                        val st = if (i == 4) {
                            styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_END_ROW }.cellStyle
                        } else {
                            styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_MIDDLE_ROW }.cellStyle
                        }
                        ExcelHelper.setCellValue(dataRow, 1, st, "")
                        rowIndex++
                    }

                    rowIndex = rowNumber
                    for (item in detail.planSummaryData!!) {
                        dataRow = sheet.getRow(rowIndex) ?: sheet.createRow(rowIndex)
                        ExcelHelper.setCellValue(dataRow, 2, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_DETAIL && x.index == 2 }.cellStyle, item.title)

                        var colIndex = 3
                        for (col in columns) {
                            val value = item.quantityByCalendars?.find { x -> x.key == col.key }?.value
                            ExcelHelper.setCellValue(dataRow, colIndex, styleCollections.first { x -> x.key == PlanStyleKey.PLAN_SUMMARY_DETAIL && x.index == colIndex }.cellStyle, value)
                            colIndex++
                        }
                        rowIndex++
                    }
                    rowNumber = rowIndex
                }
            }
        }
    }
    //endregion

}