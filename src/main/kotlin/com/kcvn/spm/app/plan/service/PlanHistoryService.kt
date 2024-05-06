package com.kcvn.spm.app.plan.service

import com.kcvn.spm.app.plan.payload.request.PlanHistoryRequest
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.KeyAppSetting
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.AppSettingRepository
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
class PlanHistoryService(private val appSettingRep: AppSettingRepository) {


    fun planHistory(request: PlanHistoryRequest): BaseResponse<List<FileContentModel>> {
        val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
        val data = mutableListOf<FileContentModel>()
        if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
            val directory = pathConfig.value?.let { File(it) }

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
            val file = File(filePath)

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
        val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
        if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
            val targetDirectoryPath = pathConfig.value
            val targetFile = File("$targetDirectoryPath/${fileContentModel.fileName}")

            FileOutputStream(targetFile).use { outputStream ->
                fileContentModel.content?.let { outputStream.write(it) }
            }
        }
    }

    fun downloadFile(fileName: String): BaseResponse<FileContentModel> {
        val pathConfig = appSettingRep.findByKey(KeyAppSetting.PATH_HISTORY_PLAN)
        if (pathConfig != null && !pathConfig.value.isNullOrEmpty()) {
            val filePath = Paths.get("${pathConfig.value}/$fileName")
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