package com.kcvn.spm.app.westfactory.service

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.westfactory.payload.response.WestFactoryResponse
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.LayoutDetailResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.WestFactoryLayout
import com.kcvn.spm.repository.BacklogWhRepository
import com.kcvn.spm.repository.WestFactoryRepository
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.File
import java.io.FileNotFoundException

@Service
@Transactional
class WestFactoryService(
    private val westFactoryRepo: WestFactoryRepository,
    private val backlogWhRepo: BacklogWhRepository,
) {
    fun importExcel(file: MultipartFile): BaseResponse<Int> {
        val templateStream = this::class.java.classLoader.getResourceAsStream("assets/template/ImportWestFactoryLayoutTemplate.xlsx")
            ?: throw FileNotFoundException("ImportWestFactoryLayoutTemplate.xlsx file not found in resources.")
        val workbook = WorkbookFactory.create(file.inputStream)
        try {
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1
            val headerRow = sheet.getRow(0)
            // Tạo file tạm thời từ InputStream
            val tempFile = File.createTempFile("ImportWestFactoryLayoutTemplate", ".xlsx").apply {
                deleteOnExit()
                outputStream().use { templateStream.copyTo(it) }
            }
            if (!ExcelHelper.columnIsMatchingTemplate(tempFile.absolutePath, headerRow, 0, 36))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))
            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            val westFactoryList = mutableListOf<WestFactoryLayout>()

            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val westFactoryData = WestFactoryLayout(
                    rowNum = ExcelHelper.getCellValue(row, 0).toDoubleOrNull()?.toInt(),
                    column1 = ExcelHelper.getCellValue(row, 1),
                    column2 = ExcelHelper.getCellValue(row, 2),
                    column3 = ExcelHelper.getCellValue(row, 3),
                    column4 = ExcelHelper.getCellValue(row, 4),
                    column5 = ExcelHelper.getCellValue(row, 5),
                    column6 = ExcelHelper.getCellValue(row, 6),
                    column7 = ExcelHelper.getCellValue(row, 7),
                    column8 = ExcelHelper.getCellValue(row, 8),
                    column9 = ExcelHelper.getCellValue(row, 9),
                    column10 = ExcelHelper.getCellValue(row, 10),
                    column11 = ExcelHelper.getCellValue(row, 11),
                    column12 = ExcelHelper.getCellValue(row, 12),
                    column13 = ExcelHelper.getCellValue(row, 13),
                    column14 = ExcelHelper.getCellValue(row, 14),
                    column15 = ExcelHelper.getCellValue(row, 15),
                    column16 = ExcelHelper.getCellValue(row, 16),
                    column17 = ExcelHelper.getCellValue(row, 17),
                    column18 = ExcelHelper.getCellValue(row, 18),
                    column19 = ExcelHelper.getCellValue(row, 19),
                    column20 = ExcelHelper.getCellValue(row, 20),
                    column21 = ExcelHelper.getCellValue(row, 21),
                    column22 = ExcelHelper.getCellValue(row, 22),
                    column23 = ExcelHelper.getCellValue(row, 23),
                    column24 = ExcelHelper.getCellValue(row, 24),
                    column25 = ExcelHelper.getCellValue(row, 25),
                    column26 = ExcelHelper.getCellValue(row, 26),
                    column27 = ExcelHelper.getCellValue(row, 27),
                    column28 = ExcelHelper.getCellValue(row, 28),
                    column29 = ExcelHelper.getCellValue(row, 29),
                    column30 = ExcelHelper.getCellValue(row, 30),
                    column31 = ExcelHelper.getCellValue(row, 31),
                    column32 = ExcelHelper.getCellValue(row, 32),
                    column33 = ExcelHelper.getCellValue(row, 33),
                    column34 = ExcelHelper.getCellValue(row, 34),
                    column35 = ExcelHelper.getCellValue(row, 35)
                )
                westFactoryList.add(westFactoryData)
            }
            // xóa record
            westFactoryRepo.delete()
            val totalRecord = westFactoryRepo.saveAll(westFactoryList)
            return BaseResponse(totalRecord, CommonUtils.getMessage("Inserted"))
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }

    fun getList(request: BacklogWhSearchRequest, pageable: Pageable): BasePagingResponse<WestFactoryResponse> {
        val westFactoryData = westFactoryRepo.getList(request, pageable)
        val backlogData = backlogWhRepo.getListWithQtyGtZero()
        val locationCodes = backlogData.map { backlog -> backlog.locationCode }
        val data = westFactoryData.first.map {
            WestFactoryResponse(
                column1 = getLayoutDetailResponse(it.column1, locationCodes),
                column2 = getLayoutDetailResponse(it.column2, locationCodes),
                column3 = getLayoutDetailResponse(it.column3, locationCodes),
                column4 = getLayoutDetailResponse(it.column4, locationCodes),
                column5 = getLayoutDetailResponse(it.column5, locationCodes),
                column6 = getLayoutDetailResponse(it.column6, locationCodes),
                column7 = getLayoutDetailResponse(it.column7, locationCodes),
                column8 = getLayoutDetailResponse(it.column8, locationCodes),
                column9 = getLayoutDetailResponse(it.column9, locationCodes),
                column10 = getLayoutDetailResponse(it.column10, locationCodes),
                column11 = getLayoutDetailResponse(it.column11, locationCodes),
                column12 = getLayoutDetailResponse(it.column12, locationCodes),
                column13 = getLayoutDetailResponse(it.column13, locationCodes),
                column14 = getLayoutDetailResponse(it.column14, locationCodes),
                column15 = getLayoutDetailResponse(it.column15, locationCodes),
                column16 = getLayoutDetailResponse(it.column16, locationCodes),
                column17 = getLayoutDetailResponse(it.column17, locationCodes),
                column18 = getLayoutDetailResponse(it.column18, locationCodes),
                column19 = getLayoutDetailResponse(it.column19, locationCodes),
                column20 = getLayoutDetailResponse(it.column20, locationCodes),
                column21 = getLayoutDetailResponse(it.column21, locationCodes),
                column22 = getLayoutDetailResponse(it.column22, locationCodes),
                column23 = getLayoutDetailResponse(it.column23, locationCodes),
                column24 = getLayoutDetailResponse(it.column24, locationCodes),
                column25 = getLayoutDetailResponse(it.column25, locationCodes),
                column26 = getLayoutDetailResponse(it.column26, locationCodes),
                column27 = getLayoutDetailResponse(it.column27, locationCodes),
                column28 = getLayoutDetailResponse(it.column28, locationCodes),
                column29 = getLayoutDetailResponse(it.column29, locationCodes),
                column30 = getLayoutDetailResponse(it.column30, locationCodes),
                column31 = getLayoutDetailResponse(it.column31, locationCodes),
                column32 = getLayoutDetailResponse(it.column32, locationCodes),
                column33 = getLayoutDetailResponse(it.column33, locationCodes),
                column34 = getLayoutDetailResponse(it.column34, locationCodes),
                column35 = getLayoutDetailResponse(it.column35, locationCodes)
            )
        }
        return BasePagingResponse(
            data,
            westFactoryData.second
        )
    }

    fun getLayoutDetailResponse(value: String?, locationCodes: List<String?>): LayoutDetailResponse {
        return if (value == "E") {
            LayoutDetailResponse(value = value, color = null)
        } else {
            if (locationCodes.contains(value)) {
                // có tồn kho màu đỏ
                LayoutDetailResponse(value = value, color = "#FF0000")
            } else {
                // không tồn kho màu xanh
                LayoutDetailResponse(value = value, color = "#0000FF")
            }
        }
    }
}