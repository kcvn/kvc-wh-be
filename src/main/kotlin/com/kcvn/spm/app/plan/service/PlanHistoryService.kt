package com.kcvn.spm.app.plan.service


import com.kcvn.spm.app.plan.payload.model.ProductPlanModel
import com.kcvn.spm.app.plan.payload.request.*
import com.kcvn.spm.common.constants.*
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.payload.*
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.File
import java.io.FileOutputStream
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths

@Service
@Transactional
class PlanHistoryService(private val appSettingRep: AppSettingRepository,
                         private val planProductRep: PlanProductRepository,
                         private val planRep: PlanRepository,
//                         private val workResultRep: WorkResultRepository,
//                         private val holidaysCalenderRep: HolidaysCalenderRepository,
//                         private val planDetailRep: PlanDetailRepository,
//                         private val planProcessRep: PlanProcessRepository
                        )
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



    fun planHistory(request: PlanHistoryRequest, pageable: Pageable): BasePagingResponse<FileContentModel> {
        val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
        val data = mutableListOf<FileContentModel>()

        if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
            val directory: File?  = try {
                File(System.getProperty("user.dir") + pathConfig.value)
            } catch (e: Exception) {
                pathConfig.value?.let { File(it) }
            }

            val excelFiles = directory?.listFiles { file ->
                file.isFile && (file.name.endsWith(".xls") || file.name.endsWith(".xlsx")) && (request.fileName.isEmpty() || file.name.contains(request.fileName))
            }?.sortedByDescending { it.lastModified() }

            excelFiles?.forEach { file ->
                val fileNameParts = file.name.split("_")
                var fileStartDate: OffsetDateTime
                var fileEndDate: OffsetDateTime

                try {
                    fileStartDate = DateTimeHelper.convertStringToOffSetDateTime(fileNameParts[0], DateTimeFormat.yyyyMMdd)
                    fileEndDate = DateTimeHelper.convertStringToOffSetDateTime(fileNameParts[1], DateTimeFormat.yyyyMMdd)
                } catch (e: Exception) {

                     fileStartDate = DateTimeHelper.convertStringToOffSetDateTime(fileNameParts[0], DateTimeFormat.ddMMyyyy)
                     fileEndDate = DateTimeHelper.convertStringToOffSetDateTime(fileNameParts[1], DateTimeFormat.ddMMyyyy)

                }


                if ((request.startDate == null || !fileStartDate.isBefore(request.startDate)) &&
                    (request.endDate == null || !fileEndDate.isAfter(request.endDate))) {
                    val fileContentModel = FileContentModel(
                        fileName = file.name,
                        content = file.readBytes(),
                        time = DateTimeHelper.toTimeZone7(OffsetDateTime.ofInstant(Instant.ofEpochMilli(file.lastModified()), ZoneOffset.UTC))
                    )
                    data.add(fileContentModel)
                }
            }
        }

        val start = pageable.pageNumber * pageable.pageSize
        val end = (start + pageable.pageSize).coerceAtMost(data.size)
        val pageData = data.subList(start, end)

        return BasePagingResponse(pageData, data.size)
    }

    fun removeFile(fileName: String): BaseResponse<Boolean> {
        val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
        if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
            val filePath: String? = try {
                System.getProperty("user.dir") + "${pathConfig.value}/$fileName"
            } catch (e: Exception) {
                "${pathConfig.value}/$fileName"
            }

            val file = filePath?.let { File(it) }

            if (file != null) {
                if (file.exists()) {
                    val isDeleted = file.delete()
                    if (isDeleted) {
                        return BaseResponse(true, CommonUtils.getMessage("delete.success"))
                    }
                }
            }
        }
        return BaseResponse(false, CommonUtils.getMessage("delete.error"))
    }



    fun addFile(fileContentModel: FileContentModel, fileName: String?): BaseResponse<String> {
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
                var targetFilePath: String = try {
                    "${System.getProperty("user.dir")}${File.separator}$targetDirectoryPath${File.separator}$finalFileName"
                } catch (e: Exception) {
                    "$targetDirectoryPath${File.separator}$finalFileName"
                }

                var targetFile = File(targetFilePath)
                var counter = 1
                while (targetFile.exists()) {
                    val nameWithoutExtension = finalFileName?.substringBeforeLast(".xlsx")
                    val newFileName = "$nameWithoutExtension($counter).xlsx"
                    targetFilePath = try {
                        "${System.getProperty("user.dir")}${File.separator}$targetDirectoryPath${File.separator}$newFileName"
                    } catch (e: Exception) {
                        "$targetDirectoryPath${File.separator}$newFileName"
                    }
                    targetFile = File(targetFilePath)
                    counter++
                }

                if (!targetFile.parentFile.exists()) {
                    targetFile.parentFile.mkdirs()
                }

                FileOutputStream(targetFile).use { outputStream ->
                    fileContentModel.content?.let { outputStream.write(it) }
                }

                return BaseResponse("File added successfully")
            }
        } catch (e: Exception) {
            return BaseResponse("Error adding file: ${e.message}")
        }
        return BaseResponse("Error adding file")
    }


    fun downloadFile(fileName: String): BaseResponse<FileContentModel> {
        val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
        if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
            val filePath: Path?= try {
                Paths.get(System.getProperty("user.dir") + "${pathConfig.value}/$fileName")
            } catch (e: Exception) {
                Paths.get("${pathConfig.value}/$fileName")
            }

            if (filePath != null && Files.exists(filePath)) {
                val fileBytes = Files.readAllBytes(filePath)

                return BaseResponse(FileContentModel(
                    fileName = fileName,
                    contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
                    content = fileBytes
                ))
            }
        }
        return BaseResponse(null, "File not found $fileName")
    }

}