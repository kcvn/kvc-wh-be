package com.kcvn.spm.app.masterdata.service

import com.kcvn.spm.app.masterdata.payload.response.MasterDataSelectionResponse
import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.app.productprocess.payload.response.ExportExcelErrResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ProcessMaster
import com.kcvn.spm.model.tables.pojos.ProcessMasterData
import com.kcvn.spm.model.tables.pojos.ProductProcess
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
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@Service
@Transactional
class MasterDataService(
    private val commonCategoryRep: CommonCategoryRepository
) {
    fun getMasterDataSelection() : MasterDataSelectionResponse {
        val types = listOf(
            Constants.KHUNG_1, Constants.KHUNG_2, Constants.KHUON_DUC,
            Constants.SR_OR_NSR, Constants.LOAI_XUAT_HANG, Constants.LOAI_TAPE,
            Constants.MACHUYENDOI, Constants.MATHONGKE, Constants.TAPE_DUNG_CHUNG, Constants.RING_JIG
        )
        val data = commonCategoryRep.getByType(types)
        return MasterDataSelectionResponse(
            frame1Selections = data.filter { x -> x.type == Constants.KHUNG_1 }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            frame2Selections = data.filter { x -> x.type == Constants.KHUNG_2 }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            moldSelections = data.filter { x -> x.type == Constants.KHUON_DUC }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            srNosrSelections = data.filter { x -> x.type == Constants.SR_OR_NSR }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            exportTypeSelections = data.filter { x -> x.type == Constants.LOAI_XUAT_HANG }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            tapeTypeSelections = data.filter { x -> x.type == Constants.LOAI_TAPE }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            processConvertCodes = data.filter { x -> x.type == Constants.MACHUYENDOI }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            processStatisticCodes = data.filter { x -> x.type == Constants.MATHONGKE }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            tapeCommonSelections = data.filter { x -> x.type == Constants.TAPE_DUNG_CHUNG }.mapNotNull { x -> DropdownResponse(x.value, x.value) },
            ringJigSelections = data.filter { x -> x.type == Constants.RING_JIG }.mapNotNull { x -> DropdownResponse(x.value, x.value) }
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

        var count = 0
        val total = sheet.lastRowNum


        val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)

        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(1).cellStyle


            val requestData = ProcessMasterData()
            val cellProcessCode = row.getCell(0)
            var processCode = if (cellProcessCode.cellType == CellType.NUMERIC && cellProcessCode.numericCellValue % 1 == 0.0)
                cellProcessCode.numericCellValue.toInt().toString()
            else {
                ExcelHelper.getCellValue(row, 0)
            }

            val groupCellProcessCode = row.getCell(1)

            var groupProcessCode = if (groupCellProcessCode.cellType == CellType.NUMERIC && groupCellProcessCode.numericCellValue % 1 == 0.0)
                groupCellProcessCode.numericCellValue.toInt().toString()
            else {
                 ExcelHelper.getCellValue(row, 1)
            }

            requestData.processCode = processCode
            requestData.groupProcessCode = groupProcessCode
            requestData.type = sheet.getRow(2).getCell(1).toString()
            requestData.value = ExcelHelper.getCellValue(row, 3)
            requestData.unit = ExcelHelper.getCellValue(row, 4)

            commonCategoryRep.add(requestData)


            // val result = messageResults.joinToString(separator = "; ")

//            if (row.getCell(colIndexResult) == null) {
//                row.createCell(colIndexResult)
//            }
            // row.getCell(colIndexResult ).setCellValue(result)
            row.getCell(colIndexResult).cellStyle = style
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