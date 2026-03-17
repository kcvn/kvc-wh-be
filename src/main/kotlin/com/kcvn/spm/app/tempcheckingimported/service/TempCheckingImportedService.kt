package com.kcvn.spm.app.tempcheckingimported.service

import com.kcvn.spm.app.tempcheckingimported.payload.response.TempCheckingImportedResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempCheckingImported
import com.kcvn.spm.repository.TempCheckingImportedRepository
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.math.BigDecimal

@Service
@Transactional
class TempCheckingImportedService(private val tempCheckingImportedRepo: TempCheckingImportedRepository) {
    fun getListFormCodeDropdown(formStatus: String, isIncludeGe3Days: Boolean): BaseResponse<List<DropdownResponse>> {
        val listFormCode = tempCheckingImportedRepo.getListFormCode(formStatus, isIncludeGe3Days)

        // Map DropDownResponse
        val dropDownList= listFormCode.map { formCode ->
            DropdownResponse(
                formCode,
                formCode
            )
        }.toMutableList()
        if (!isIncludeGe3Days) dropDownList.add(DropdownResponse("File lam tem goi", "File lam tem goi"))

        return BaseResponse(data = dropDownList)
    }

    fun getList(formCode: String): BasePagingResponse<TempCheckingImportedResponse> {
        val data = tempCheckingImportedRepo.getList(formCode)
        val response = data.first.map {
            TempCheckingImportedResponse(
                poNumber = it.poNumber,
                qty = it.qty,
                formCode = it.formCode
            )
        }
        return BasePagingResponse(
            response,
            data.second
        )
    }

    fun importChecking(formCode: String, file: MultipartFile): BaseResponse<Int> {
        // validate formCode
        val entity = tempCheckingImportedRepo.findByFormCode(formCode)
        if (entity != null) {
            throw BusinessException(CommonUtils.getMessage("form.code.error.nameTaken"))
        }

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/TempCheckingImportedTemplate.xlsx"
        val workbook = WorkbookFactory.create(file.inputStream)
        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 5
            val headerRow = sheet.getRow(4)
            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 4, 4))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))
            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            val dataList = mutableListOf<TempCheckingImported>()

            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val poNumber = ExcelHelper.getCellValueAmoeba(row, 2).trim()
                val qtyStr = ExcelHelper.getCellValueAmoeba(row, 3).trim()

                if (poNumber.isBlank() && qtyStr.isBlank()) {
                    continue
                }

                val data = TempCheckingImported(
                    poNumber = ExcelHelper.getCellValueAmoeba(row, 2),
                    qty = ExcelHelper.getCellValueAmoeba(row, 3).toBigDecimalOrNull() ?: BigDecimal.ZERO,
                    formCode = formCode
                )
                dataList.add(data)
            }
            // save temp checking imported
            val totalRecord = tempCheckingImportedRepo.saveAll(dataList)

            return BaseResponse(totalRecord, CommonUtils.getMessage("action.succeeded"))
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }

    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/TempCheckingImportedTemplate.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.use { it.write(byteArrayOutputStream) }

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("TempCheckingImportedTemplate.xlsx"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = byteArrayOutputStream.toByteArray()
        )

        return BaseResponse(response)
    }
}