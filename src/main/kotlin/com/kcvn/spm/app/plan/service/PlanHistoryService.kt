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
import java.time.format.DateTimeFormatter

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
        val dateFormat = DateTimeFormatter.ofPattern("ddMMyyyy")

        if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
            val directory = pathConfig.value?.let { File(System.getProperty("user.dir") + it) }

            val excelFiles = directory?.listFiles { file ->
                file.isFile && (file.name.endsWith(".xls") || file.name.endsWith(".xlsx")) && (request.fileName.isEmpty() || file.name.contains(request.fileName))
            }

            if (excelFiles != null) {
                for (file in excelFiles) {
                    val fileNameParts = file.name.split("_")
                    val fileStartDate = OffsetDateTime.parse(fileNameParts[0], dateFormat)
                    val fileEndDate = OffsetDateTime.parse(fileNameParts[1], dateFormat)

                    if ((request.startDate == null || !fileStartDate.isBefore(request.startDate)) &&
                        (request.endDate == null || !fileEndDate.isAfter(request.endDate))) {
                        val fileContentModel = FileContentModel(
                            fileName = file.name,
                            content = file.readBytes(),
                            time = OffsetDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneOffset.UTC)
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



    fun addFile(fileContentModel: FileContentModel, fileName: String?) {
        try {
            val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
            if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
                val targetDirectoryPath = pathConfig.value
                var finalFileName = fileName
                if (fileName != null) {
                    if (!fileName.endsWith(".xlsx")) {
                        finalFileName += ".xlsx"
                    }
                }
                val targetFilePath = "${System.getProperty("user.dir")}${File.separator}$targetDirectoryPath${File.separator}$finalFileName"
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