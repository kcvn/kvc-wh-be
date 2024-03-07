package com.kcvn.spm.app.equipmentproductivity.service

import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.EquipmentProductivityRepository
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

@Service
@Transactional
class EquipmentProductivityService(equipmentProductivityRepository: EquipmentProductivityRepository)
{
    fun importExcel(file: MultipartFile, startDate: OffsetDateTime, endDate:OffsetDateTime) : BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1
        var count = 0
        val total = sheet.lastRowNum - rowIndex
        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            count++
        }
        return BaseResponse(null, CommonUtils.getMessage("Insert Ok", arrayOf(count, total + 1)))
    }
}