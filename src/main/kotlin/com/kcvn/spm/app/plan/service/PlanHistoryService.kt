package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.model.PlanDataByProcessModel
import com.kcvn.spm.app.plan.payload.model.ProcessChildrenModel
import com.kcvn.spm.app.plan.payload.model.ProductPlanDetailModel
import com.kcvn.spm.app.plan.payload.model.ProductPlanModel
import com.kcvn.spm.app.plan.payload.request.*
import com.kcvn.spm.app.plan.payload.response.ProductPlanDetailResponse
import com.kcvn.spm.common.constants.*
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.payload.*
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.FileNotFoundException
import java.io.FileOutputStream
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.nio.file.Files
import java.nio.file.Paths

@Service
@Transactional
class PlanHistoryService(private val appSettingRep: AppSettingRepository,
                         private val planProductRep: PlanProductRepository,
                         private val planRep: PlanRepository,
                         private val workResultRep: WorkResultRepository,
                         private val holidaysCalenderRep: HolidaysCalenderRepository,
                         private val planDetailRep: PlanDetailRepository,
                         private val planProcessRep: PlanProcessRepository)
{

    fun getPlansByMonth(month: String): BaseResponse<List<DropdownResponse>>{
        val parts = month.split("/")
        val monthPart = parts[0].toInt()
        val yearPart = parts[1].toInt()
        val plans = planRep.getListPlanByMonth(monthPart, yearPart)
        val response = mutableListOf<DropdownResponse>()
        for(plan in plans){
            if(plan.isActive == false){
                response.add(DropdownResponse(value = plan.id, label = "V"+plan.version.toString()))
            }
            else{
                response.add(DropdownResponse(value = plan.id, label = PlanVersion.LATEST))
            }
        }
        return BaseResponse(response)
    }
    fun getPlanDetail(request: PlanHistoryDetailRequest): ProductPlanDetailResponse {
        val response = ProductPlanDetailResponse()
        val plan = planRep.getPlanById(request.planId)
        val startDate = plan?.startDate
        val endDate = plan?.endDate
        if (request.productName.isEmpty()) throw BusinessException(CommonUtils.getMessage("plan.invalidParam"))
        if (startDate == null || endDate == null) throw BusinessException(CommonUtils.getMessage("plan.invalidTime"))

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        response.columns = DateTimeHelper.toCalendarColumn(
            DateTimeHelper.toTimeZone7(startDate)!!,
            DateTimeHelper.toTimeZone7(endDate)!!,
            holidayCalenders
        )
        val planDetailRequest = PlanDetailRequest(
            productName = request.productName,
            startDate = startDate,
            endDate = endDate
        )
        val planProducts = planProductRep.getForPlan(planDetailRequest)
        if (planProducts.isEmpty()) throw BusinessException(CommonUtils.getMessage("data.notExist"))
        val planProductIds = planProducts.mapNotNull { it.id }
        val productNames = planProducts.mapNotNull { it.productName }.distinct()

        val data = mutableListOf<ProductPlanDetailModel>()

        val planProcesses = planProcessRep.getListPlanProcess(planProductIds,  false)
        var parentPlanProcesses = planProcesses.filter { x -> x.parentId.isNullOrEmpty() }
        val childrenPlanProcesses = planProcesses.filter { x -> x.parentId != null }

        var planProcessIds = parentPlanProcesses.mapNotNull { x -> x.id }
        val planDetails = planDetailRep.getPlanDetail(
            planProcessIds, startDate, endDate,
            false, isDraft = false
        )

        planProcessIds = planDetails.mapNotNull { x -> x.planProcessId }
        parentPlanProcesses = parentPlanProcesses.filter { x -> planProcessIds.any { m -> m == x.id } }

        val workResults = workResultRep.getForPlan(startDate, endDate, productNames)

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
                    KeyValueResponse(
                        DateTimeHelper.toString(DateTimeHelper.toTimeZone7(m.key)!!, DateTimeFormat.yyyyMMdd),
                        if (x.unit == ProcessUnit.BLOCK) m.value.sumOf { it.blockQuantity ?: 0 }.toString() else m.value.sumOf { it.sheetQuantity ?: 0 }.toString()
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
                            KeyValueResponse(
                                m.key,
                                m.value.sumOf { it.value?.toInt() ?: 0 }.toString(),
                            )
                        },
                        sort = x.value.first().sort
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
    private fun calculateAccumulation(data: List<KeyValueResponse>, firstValue: Int? = null): List<KeyValueResponse> {
        var value = firstValue ?: 0
        val response = mutableListOf<KeyValueResponse>()
        for (item in data) {
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
    fun getListPlanByVersion(request: PlanHistorySearchRequest, pageable: Pageable): BasePagingResponse<ProductPlanModel> {
        val data = planProductRep.getListPlanHistory(request, pageable)
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


    fun planHistory(request: PlanHistoryRequest): BaseResponse<List<FileContentModel>> {

        val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
        val data = mutableListOf<FileContentModel>()
        if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
            val directory = pathConfig.value?.let { File(System.getProperty("user.dir") +it) }

            val excelFiles = directory?.listFiles { file ->
                file.isFile && (file.name.endsWith(".xls") || file.name.endsWith(".xlsx")) && (request.fileName.isEmpty() || file.name.contains(request.fileName))
            }

            if (excelFiles != null) {
                for (file in excelFiles) {
                    val lastModified = file.lastModified()
                    val lastModifiedTime = OffsetDateTime.ofInstant(Instant.ofEpochMilli(lastModified), ZoneOffset.UTC)

                    if ((request.startDate == null || lastModifiedTime.isAfter(request.startDate)) &&
                        (request.endDate == null || lastModifiedTime.isBefore(request.endDate))) {
                        val fileContentModel = FileContentModel(
                            fileName = file.name,
                            content = file.readBytes(),
                            time = lastModifiedTime
                        )
                        data.add(fileContentModel)
                    }
                }
            }
        }
        return BaseResponse(data)
    }

    fun removeFile(fileName: String): BaseResponse<Boolean> {
        val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
        if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
            val filePath = "${pathConfig.value}/$fileName"
            val file = File(System.getProperty("user.dir")+filePath)

            if (file.exists()) {
                val isDeleted = file.delete()
                if (isDeleted) {
                    return BaseResponse(true, CommonUtils.getMessage("detete.success"))
                }
            }
        }
        throw BusinessException(CommonUtils.getMessage("delete.error"))
    }



    fun addFile(fileContentModel: FileContentModel) {
        try {
            val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
            if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
                val targetDirectoryPath = pathConfig.value
                val targetFilePath = "${System.getProperty("user.dir")}${File.separator}$targetDirectoryPath${File.separator}${fileContentModel.fileName}"
                val targetFile = File(targetFilePath)

                if (!targetFile.parentFile.exists()) {
                    targetFile.parentFile.mkdirs()
                }

                FileOutputStream(targetFile).use { outputStream ->
                    fileContentModel.content?.let { outputStream.write(it) }
                }
            }
        } catch (e: Exception) {
            throw BusinessException(e.message)
        }
    }


    fun downloadFile(fileName: String): BaseResponse<FileContentModel> {
        val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
        if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
            val filePath = Paths.get(System.getProperty("user.dir")+"${pathConfig.value}/$fileName")
            val fileBytes = Files.readAllBytes(filePath)

            return BaseResponse(FileContentModel(
                fileName = fileName,
                contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
                content = fileBytes
            ))
        }
        throw FileNotFoundException("File not found $fileName")
    }

}