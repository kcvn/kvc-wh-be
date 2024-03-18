package com.kcvn.spm.app.masterdata.service

import com.kcvn.spm.app.masterdata.payload.response.MasterDataSelectionResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.MasterDataType
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ProcessMasterData
import com.kcvn.spm.repository.CommonCategoryRepository
import org.apache.poi.ss.usermodel.CellType
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class MasterDataService(
    private val commonCategoryRep: CommonCategoryRepository
) {
    fun getMasterDataSelection() : MasterDataSelectionResponse {
        val types = listOf(
            MasterDataType.KHUNG_1, MasterDataType.KHUNG_2, MasterDataType.KHUON_DUC,
            MasterDataType.SR_OR_NSR, MasterDataType.LOAI_XUAT_HANG, MasterDataType.LOAI_TAPE,
            MasterDataType.MACHUYENDOI, MasterDataType.MATHONGKE, MasterDataType.TAPE_DUNG_CHUNG, MasterDataType.RING_JIG
        )
        val data = commonCategoryRep.getByType(types)
        return MasterDataSelectionResponse(
            frame1Selections = data.filter { x -> x.type == MasterDataType.KHUNG_1 }.map { x -> DropdownResponse(x.value, x.value) },
            frame2Selections = data.filter { x -> x.type == MasterDataType.KHUNG_2 }.map { x -> DropdownResponse(x.value, x.value) },
            moldSelections = data.filter { x -> x.type == MasterDataType.KHUON_DUC }.map { x -> DropdownResponse(x.value, x.value) },
            srNosrSelections = data.filter { x -> x.type == MasterDataType.SR_OR_NSR }.map { x -> DropdownResponse(x.value, x.value) },
            exportTypeSelections = data.filter { x -> x.type == MasterDataType.LOAI_XUAT_HANG }.map { x -> DropdownResponse(x.value, x.value) },
            tapeTypeSelections = data.filter { x -> x.type == MasterDataType.LOAI_TAPE }.map { x -> DropdownResponse(x.value, x.value) },
            processConvertCodes = data.filter { x -> x.type == MasterDataType.MACHUYENDOI }.map { x -> DropdownResponse(x.value, x.value) },
            processStatisticCodes = data.filter { x -> x.type == MasterDataType.MATHONGKE }.map { x -> DropdownResponse(x.value, x.value) },
            tapeCommonSelections = data.filter { x -> x.type == MasterDataType.TAPE_DUNG_CHUNG }.map { x -> DropdownResponse(x.value, x.value) },
            ringJigSelections = data.filter { x -> x.type == MasterDataType.RING_JIG }.map { x -> DropdownResponse(x.value, x.value) }
        )
    }

    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProcessMasterData.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = "ImportProcessMasterData",
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }

    fun importExcelProcessMasterData(file: MultipartFile): BaseResponse<FileContentModel> {
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1

        if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
            throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val headerRow = sheet.getRow(0)

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportProcessMasterData.xlsx"

        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 5))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

        val count = 0
        val total = sheet.lastRowNum


        val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {



            val requestData = ProcessMasterData()
            val cellProcessCode = row.getCell(0)
            val processCode = if (cellProcessCode.cellType == CellType.NUMERIC && cellProcessCode.numericCellValue % 1 == 0.0)
                cellProcessCode.numericCellValue.toInt().toString()
            else {
                ExcelHelper.getCellValue(row, 0)
            }

            val groupCellProcessCode = row.getCell(1)

            val groupProcessCode = if (groupCellProcessCode.cellType == CellType.NUMERIC && groupCellProcessCode.numericCellValue % 1 == 0.0)
                groupCellProcessCode.numericCellValue.toInt().toString()
            else {
                 ExcelHelper.getCellValue(row, 1)
            }

            requestData.processCode = processCode
            requestData.groupProcessCode = groupProcessCode
            requestData.type = sheet.getRow(1).getCell(2).toString()
            requestData.value = ExcelHelper.getCellValue(row, 3)
            requestData.unit = ExcelHelper.getCellValue(row, 4)

            commonCategoryRep.add(requestData)


            // val result = messageResults.joinToString(separator = "; ")

//            if (row.getCell(colIndexResult) == null) {
//                row.createCell(colIndexResult)
//            }
            // row.getCell(colIndexResult ).setCellValue(result)

        }


        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()



        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.excel.result.import", arrayOf(
                LocalDateTime.now().format(
                DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )
        workbook.close()

        return BaseResponse(
            response,
            if (count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("import.success", arrayOf(count, total))
        )
    }

}