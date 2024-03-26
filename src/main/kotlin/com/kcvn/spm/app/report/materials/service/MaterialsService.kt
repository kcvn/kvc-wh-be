package com.kcvn.spm.app.report.materials.service

import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.FileInputStream

@Service
@Transactional
class MaterialsService {
    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportTape.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importProductProcessTemplate"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }
}