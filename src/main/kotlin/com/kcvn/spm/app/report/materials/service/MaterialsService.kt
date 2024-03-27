package com.kcvn.spm.app.report.materials.service

import com.kcvn.spm.app.report.materials.payload.request.ImportTapeRequest
import com.kcvn.spm.app.report.materials.payload.response.ImportTapeErrResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.OrderInfoRepository
import com.kcvn.spm.repository.TapeRepository
import org.apache.poi.ss.usermodel.WorkbookFactory
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.FileInputStream
import java.time.OffsetDateTime
import java.time.Year
import java.time.temporal.ChronoUnit

@Service
@Transactional
class MaterialsService(
    private val tapeInfoRep : TapeRepository,
    private val orderInfoRep : OrderInfoRepository
) {
    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportTape.xlsx"
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.importTape"),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }

    fun importExcelTape (request: ImportTapeRequest,
                         file: MultipartFile): BaseResponse<FileContentModel> {
        // Validate ngày yêu cầu đơn hàng với tháng báo cáo

        val monthStartDate = request.startDate?.plusHours(7)?.monthValue ?: 0
        val monthEndDate = request.endDate?.plusHours(7)?.monthValue ?: 0
        val yearStartDate = request.startDate?.plusHours(7)?.year ?: 0
        val yearEndDate = request.endDate?.plusHours(7)?.year ?: 0
        val monthReport = request.monthReport?.toIntOrNull() ?: 0
        val yearReport = request.yearReport?.toIntOrNull() ?: 0
        if(request.monthReport?.toInt() == 1){
            if(!((monthStartDate != 12 && monthStartDate == 1 && yearStartDate == yearReport)
                || (monthStartDate == 12 && yearReport - yearStartDate == 1))){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.startDate"))
                }
        }else {
            if(!((monthReport - monthStartDate == 1 || monthReport == monthStartDate) && yearReport == yearStartDate)){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.startDate"))
            }
        }
        if(monthReport == 12){
            if(!((monthEndDate != 1 && monthEndDate == 12 && yearEndDate == yearReport)
                        || (monthEndDate == 1 && yearEndDate - yearReport.toInt()  == 1))){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.endDate"))
            }
        }else {
            if(!((monthEndDate - monthReport  == 1 || monthReport == monthEndDate) && yearReport == yearEndDate)){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.endDate"))
            }
        }
        // Validate thời gian yêu cầu của các tháng báo cáo phải là liên tiếp

        var monthPre = "";
        var yearPre = "";
        var monthNext = "";
        var yearNext = "";
        // Kiểm tra xem tháng của import vào trường hợp đặc biệt tháng 12 và 1 thì phải sang năm mới

        if(monthReport.toInt() == 1){
            monthPre = "12"
            yearPre = (yearReport.toInt() - 1).toString()
        }else {
            monthPre = (monthReport.toInt() - 1).toString()
            yearPre = yearReport.toString()
        }
        if(monthReport.toInt() == 12){
            monthNext = "1"
            yearNext = (yearReport.toInt() + 1).toString()
        }else {
            monthNext = (monthReport.toInt() + 1).toString()
            yearNext = yearReport.toString()
        }
        // check xem có tồn tại dữ liệu của tháng trước không

        val queryTapePre = tapeInfoRep.getTapeDetailByMonth(monthPre,yearPre)
        if(queryTapePre != null){
            val subtraction = request.startDate?.until(queryTapePre.requestDateEnd, ChronoUnit.DAYS)
            if(subtraction != 1L){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.orderRequestDate"))
            }
        }
        // check xem có tồn tại dữ liệu của tháng sau không

        val queryTapeNext = tapeInfoRep.getTapeDetailByMonth(monthNext,yearNext)
        if(queryTapeNext != null){
            val subtraction = queryTapeNext.requestDateStart!!.until(request.endDate, ChronoUnit.DAYS)
            if(subtraction != 1L){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.orderRequestDate"))
            }
        }

        // Xử lý file import
        val workbook = WorkbookFactory.create(file.inputStream)
        val sheet = workbook.getSheetAt(0)
        val rowIndex = 1
        if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
            throw BusinessException(CommonUtils.getMessage("import.file.empty"))

        val headerRow = sheet.getRow(0)

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportTape.xlsx"

        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 5))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

        val total = sheet.lastRowNum
        // Lấy ra list sản phẩm trong bảng đơn hàng
        val listProductDataBase = orderInfoRep.getProductNameByOder(request.startDate, request.endDate)

        // Tạo list để lưu tên sản phẩm trong file import
        val listProductFileImport : MutableList<String> = mutableListOf()

        // List lưu những sản phẩm bị lỗi
        val listProductErr : MutableList<ImportTapeErrResponse> = mutableListOf()
        // Đọc file import
        val isErr = false
        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val style = row.getCell(0).cellStyle
        }
        // Xóa toàn bộ các bản ghi trong tháng nếu tháng đó đã có dữ liệu
        if(request.hasUpdateTape){

        }
        return BaseResponse(
        )
    }
}