package com.kcvn.spm.app.workresult.service

import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.app.workresult.payload.response.PagingWorkResultResponse
import com.kcvn.spm.app.workresult.payload.response.WorkResultResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.constants.ProcessUnit
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.repository.ProcessMasterRepository
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
    private val processMasterRep: ProcessMasterRepository
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
        val processCodes = workResults.mapNotNull { x -> x.processCode }.distinct()
        val processMasterData = processMasterRep.getProcessMasterDataByCode(processCodes)
        val processMasterList = processMasterRep.getByProcessCode(processCodes)
        response.data = workResults.map { x ->
            val processMaster = processMasterData.find { m -> m.processCode == x.processCode }
            val process = processMasterList.find { m -> m.processCode == x.processCode }
            var result = 0.0
            if (processMaster != null) {
                when (processMaster.unit) {
                    ProcessUnit.BLOCK -> result = (x.goodTapeQuantity?.toDouble())?.div(x.totalTapeQuantity!!) ?: 0.0
                    ProcessUnit.SHEET -> result = (x.goodSheetQuantity?.toDouble())?.div(x.totalSheetQuantity!!) ?: 0.0
                }
            }

            val percentage = result.times(100)
            val performance = if (percentage == 0.0) "" else "%.2f%%".format(percentage)

            WorkResultResponse(
                id = x.id,
                actualResultDepartment = x.actualResultDepartment,
                team = x.team,
                processCode = x.processCode,
                processName = x.processName,
                processNameJp = process?.processNameJp,
                summaryResultDate = x.summaryResultDate,
                workStartTime = x.workStartTime,
                workEndTime = x.workEndTime,
                orderCode = x.orderCode,
                itemName = x.itemName,
                customerCode = x.customerCode,
                remediationDirectiveNumber = x.remediationDirectiveNumber,
                tapeLotNo = x.tapeLotNo,
                code = x.code,
                layerCode = x.layerCode,
                total = x.total,
                totalTapeQuantity = x.totalTapeQuantity,
                totalSheetQuantity = x.totalSheetQuantity,
                goodItemQuantity = x.goodItemQuantity,
                goodTapeQuantity = x.goodTapeQuantity,
                goodSheetQuantity = x.goodSheetQuantity,
                performance = performance,
                departmentCode = x.departmentCode,
                workImplementBy = x.workImplementBy,
                equipmentName = x.equipmentName,
                description = x.description
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

                ExcelHelper.setCellValue(dataRow, 0, style, item.actualResultDepartment)
                ExcelHelper.setCellValue(dataRow, 1, style, item.team)
                ExcelHelper.setCellValue(dataRow, 2, style, item.processCode)
                ExcelHelper.setCellValue(dataRow, 3, style, item.processName)
                ExcelHelper.setCellValue(dataRow, 4, style, item.processNameJp) // cai nay khong biet lay tu dau
                ExcelHelper.setCellValue(dataRow, 5, style, item.summaryResultDate?.format(formatter).toString())
                ExcelHelper.setCellValue(dataRow, 6, style, item.workStartTime?.format(formatter).toString())
                ExcelHelper.setCellValue(dataRow, 7, style, item.workEndTime?.format(formatter).toString())
                ExcelHelper.setCellValue(dataRow, 8, style, item.orderCode)
                ExcelHelper.setCellValue(dataRow, 9, style, item.itemName)
                ExcelHelper.setCellValue(dataRow, 10, style, item.customerCode)
                ExcelHelper.setCellValue(dataRow, 11, style, item.remediationDirectiveNumber)
                ExcelHelper.setCellValue(dataRow, 12, style, item.tapeLotNo)
                ExcelHelper.setCellValue(dataRow, 13, style, "") // chiu 14
                ExcelHelper.setCellValue(dataRow, 14, style, item.code)
                ExcelHelper.setCellValue(dataRow, 15, style, item.layerCode)
                ExcelHelper.setCellValue(dataRow, 16, style, item.total?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 17, style, item.totalTapeQuantity?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 18, style, item.totalSheetQuantity?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 19, style, item.goodItemQuantity?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 20, style, item.goodTapeQuantity?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 21, style, item.goodSheetQuantity?.toString() ?: "")
                ExcelHelper.setCellValue(dataRow, 22, style, performance)
                ExcelHelper.setCellValue(dataRow, 23, style, "") // chiu 24
                ExcelHelper.setCellValue(dataRow, 24, style, item.departmentCode)
                ExcelHelper.setCellValue(dataRow, 25, style, item.workImplementBy)
                ExcelHelper.setCellValue(dataRow, 26, style, item.equipmentName)
                ExcelHelper.setCellValue(dataRow, 27, style, item.description)

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