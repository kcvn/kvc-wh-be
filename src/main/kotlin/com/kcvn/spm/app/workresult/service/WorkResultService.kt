package com.kcvn.spm.app.workresult.service

import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.app.workresult.payload.response.PagingWorkResultResponse
import com.kcvn.spm.app.workresult.payload.response.WorkResultResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.repository.WorkResultRepository
import org.apache.poi.ss.usermodel.BorderStyle
import org.apache.poi.ss.usermodel.CellStyle
import org.apache.poi.ss.usermodel.Font
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
            val style: CellStyle = workBook.createCellStyle()
            style.borderBottom = BorderStyle.THIN
            style.borderTop = BorderStyle.THIN
            style.borderRight = BorderStyle.THIN
            style.borderLeft = BorderStyle.THIN
            style.wrapText = true

            val font: Font = workBook.createFont()
            font.fontName = ExcelConstant.FONT_TIMES_NEW_ROMAN
            font.fontHeightInPoints = 12.toShort()
            style.setFont(font)

            var rowNumber = 2
            for (item in workResultMapping.data!!) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                dataRow.createCell(0).setCellValue(item.summaryResultDate?.format(formatter).toString())
                dataRow.getCell(0).cellStyle = style

                dataRow.createCell(1).setCellValue(item.itemName)
                dataRow.getCell(1).cellStyle = style

                dataRow.createCell(2).setCellValue(item.processName)
                dataRow.getCell(2).cellStyle = style

                dataRow.createCell(3).setCellValue(item.processCode)
                dataRow.getCell(3).cellStyle = style

                dataRow.createCell(4).setCellValue(item.layerCode)
                dataRow.getCell(4).cellStyle = style

                val totalTapeQuantity = item.totalTapeQuantity?.toString() ?: ""

                dataRow.createCell(5).setCellValue(totalTapeQuantity)
                dataRow.getCell(5).cellStyle = style

                val totalSheetQuantity = item.totalSheetQuantity?.toString() ?: ""

                dataRow.createCell(6).setCellValue(totalSheetQuantity)
                dataRow.getCell(6).cellStyle = style

                val goodTapeQuantity = item.goodTapeQuantity?.toString() ?: ""

                dataRow.createCell(7).setCellValue(goodTapeQuantity)
                dataRow.getCell(7).cellStyle = style

                val goodSheetQuantity = item.goodSheetQuantity?.toString() ?: ""

                dataRow.createCell(8).setCellValue(goodSheetQuantity)
                dataRow.getCell(8).cellStyle = style

                val result = (item.goodSheetQuantity?.toDouble())?.div(item.totalSheetQuantity!!)

                val percentage = result?.times(100)

                var performance = "%.2f%%".format(percentage)

                if (result == null || item.goodSheetQuantity == 0 || item.totalSheetQuantity == 0) {
                    performance = ""
                }

                dataRow.createCell(9).setCellValue(performance)
                dataRow.getCell(9).cellStyle = style

                dataRow.createCell(10).setCellValue(item.orderCode)
                dataRow.getCell(10).cellStyle = style

                dataRow.createCell(11).setCellValue(item.tapeLotNo)
                dataRow.getCell(11).cellStyle = style

                dataRow.createCell(12).setCellValue(item.code)
                dataRow.getCell(12).cellStyle = style

                dataRow.createCell(13).setCellValue(item.workImplementBy)
                dataRow.getCell(13).cellStyle = style

                dataRow.createCell(14).setCellValue(item.equipmentName)
                dataRow.getCell(14).cellStyle = style
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