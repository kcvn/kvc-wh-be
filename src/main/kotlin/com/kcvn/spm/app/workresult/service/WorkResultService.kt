package com.kcvn.spm.app.workresult.service

import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.app.workresult.payload.response.PagingWorkResultResponse
import com.kcvn.spm.app.workresult.payload.response.WorkResultResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.repository.WorkResultRepository
import org.apache.poi.ss.usermodel.Row
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

@Service
@Transactional
class WorkResultService(
    private val workResultRep: WorkResultRepository,
) {
    fun getListWorkResult(
        request: WorkResultSearchRequest?,
        pageable: Pageable,
    ): BasePagingResponse<WorkResultResponse> {
        val workResults = workResultRep.getPagingListWorkResult(request, pageable)
        var response = BasePagingResponse<WorkResultResponse>()

        if (workResults.first.isNotEmpty()) {
            response = mappingWorkResultResponse(workResults.first)
            response.totalRecords = workResults.second
        }


        return response
    }

    private fun mappingWorkResultResponse(workResults: List<WorkResult>): BasePagingResponse<WorkResultResponse> {
        val response = PagingWorkResultResponse()
        response.data = workResults.map { x ->
            val result = (x.goodSheetQuantity?.toDouble())?.div(x.totalSheetQuantity!!)
            val percentage = result?.times(100)
            var performance = "%.2f%%".format(percentage)
            if (result == null || x.goodSheetQuantity == 0 || x.totalSheetQuantity == 0) {
                performance = ""
            }
            WorkResultResponse(
                id = x.id,
                summaryResultDate = x.summaryResultDate,
                itemName = x.itemName,
                processName = x.processName,
                processCode = x.processCode,
                layerCode = x.layerCode,
                totalTapeQuantity = x.totalTapeQuantity,
                totalSheetQuantity = x.totalSheetQuantity,
                goodTapeQuantity = x.goodTapeQuantity,
                goodSheetQuantity = x.goodSheetQuantity,
                performance = performance,
                orderCode = x.orderCode,
                tapeLotNo = x.tapeLotNo,
                code = x.code,
                workImplementBy = x.workImplementBy,
                equipmentName = x.equipmentName
            )
        }

        return response
    }

    fun getListProcessGroup(): BaseResponse<List<DropdownResponse>> {
        val listProcessGroup = workResultRep.getListProcessGroup()

        // Map each ProcessGroupResponse to a DropDownResponse
        val dropDownList: List<DropdownResponse> = listProcessGroup.map { processGroupResponse ->
            DropdownResponse(
                processGroupResponse.grpProcess,
                "${processGroupResponse.grpProcess} - ${processGroupResponse.processName}"
            )
        }

        return BaseResponse(data = dropDownList)
    }


    fun getListProcessByGroupCode(groupCodes: String): BaseResponse<List<DropdownResponse>> {
        val listProcessByGroupCode = workResultRep.getListProcessByGroupCode(groupCodes)

        // Map each ProcessResponse to a DropdownResponse
        val dropDownList: List<DropdownResponse> = listProcessByGroupCode.map { processResponse ->
            DropdownResponse(
                processResponse.processCode,
                "${processResponse.processCode} - ${processResponse.processName}"
            )
        }

        return BaseResponse(data = dropDownList)
    }

    fun exportExcel(request: WorkResultSearchRequest?, pageable: Pageable): BaseResponse<FileContentModel> {
        val workResults = workResultRep.getList(request, pageable)
        val workResultMapping = mappingWorkResultResponse(workResults)

        val fileTemplate =
            File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportWorkResultTemplate.xlsx")
        val workBook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workBook.getSheetAt(0)

        if (!workResultMapping.data.isNullOrEmpty()) {
            val style = ExcelHelper.getCellStyleCommon(workBook)

            var rowNumber = 2
            for (item in workResultMapping.data!!) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")

                val result = (item.goodSheetQuantity?.toDouble())?.div(item.totalSheetQuantity!!)
                val percentage = result?.times(100)
                var performance = "%.2f%%".format(percentage)
                if (result == null || item.goodSheetQuantity == 0 || item.totalSheetQuantity == 0) {
                    performance = ""
                }

                ExcelHelper.setCellValue(dataRow, 0, style, item.summaryResultDate?.format(formatter).toString())
                ExcelHelper.setCellValue(dataRow, 1, style, item.itemName)
                ExcelHelper.setCellValue(dataRow, 2, style, item.processName)
                ExcelHelper.setCellValue(dataRow, 3, style, item.processCode)
                ExcelHelper.setCellValue(dataRow, 4, style, item.layerCode)
                ExcelHelper.setCellValue(dataRow, 5, style, item.totalTapeQuantity?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 6, style, item.totalSheetQuantity?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 7, style, item.goodTapeQuantity?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 8, style, item.goodSheetQuantity?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 9, style, performance)
                ExcelHelper.setCellValue(dataRow, 10, style, item.orderCode)
                ExcelHelper.setCellValue(dataRow, 11, style, item.tapeLotNo)
                ExcelHelper.setCellValue(dataRow, 12, style, item.code)
                ExcelHelper.setCellValue(dataRow, 13, style, item.workImplementBy)
                ExcelHelper.setCellValue(dataRow, 14, style, item.equipmentName)
            }
        }
        val byteArrayOutputStream = ByteArrayOutputStream()
        workBook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()
        val response = FileContentModel(
            fileName = CommonUtils.getMessage(
                "fileName.exportListWorkResult",
                arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))
            ),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workBook.close()

        return BaseResponse(response)
    }


    fun createResponseEntity(
        report: ByteArray?,
        fileName: String?,
    ): ResponseEntity<ByteArray> =
        ResponseEntity.ok()
            .contentType(MediaType.APPLICATION_OCTET_STREAM)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"$fileName\"")
            .body(report)

}