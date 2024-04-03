package com.kcvn.spm.app.report.materials.service

import com.kcvn.spm.app.report.materials.payload.response.CheckImportTapeResponse
import com.kcvn.spm.app.masterdata.service.MasterDataService
import com.kcvn.spm.app.report.materials.payload.request.GetReportMaterialsRequest
import com.kcvn.spm.app.report.materials.payload.request.ImportTapeRequest
import com.kcvn.spm.app.report.materials.payload.response.ImportTapeErrResponse
import com.kcvn.spm.common.constants.ExcelConstant
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.CellStyleModel
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TapeInfo
import com.kcvn.spm.repository.CompletionRateProductRepository
import com.kcvn.spm.repository.OrderInfoRepository
import com.kcvn.spm.repository.TapeRepository
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.math.BigDecimal
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import kotlin.math.round

@Service
@Transactional
class MaterialsService(
    private val tapeInfoRep : TapeRepository,
    private val orderInfoRep : OrderInfoRepository,
    private val completionRateProductRepository: CompletionRateProductRepository,
    private val masterDataService: MasterDataService,
) {
    fun downloadTemplate(): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/Import_GiaTape_template.xlsx"
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

    fun  importExcelTape (request: ImportTapeRequest,
                         file: MultipartFile): BaseResponse<FileContentModel> {
        // Validate ngày yêu cầu đơn hàng với tháng báo cáo

        val monthStartDate = request.startDate?.plusHours(7)?.monthValue ?: 0
        val monthEndDate = request.endDate?.plusHours(7)?.monthValue ?: 0
        val yearStartDate = request.startDate?.plusHours(7)?.year ?: 0
        val yearEndDate = request.endDate?.plusHours(7)?.year ?: 0
        val monthReport = request.monthReport
        val yearReport = request.yearReport
        if(request.monthReport == 1){
            if(!((monthStartDate != 12 && monthStartDate == 1 && yearStartDate == yearReport)
                || (monthStartDate == 12 && yearReport!! - yearStartDate == 1))){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.startDate"))
                }
        }else {
            if(!((monthReport!! - monthStartDate == 1 || monthReport == monthStartDate) && yearReport == yearStartDate)){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.startDate"))
            }
        }
        if(monthReport == 12){
            if(!((monthEndDate != 1 && monthEndDate == 12 && yearEndDate == yearReport)
                        || (monthEndDate == 1 && yearEndDate - yearReport == 1))){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.endDate"))
            }
        }else {
            if(!((monthEndDate - (monthReport ?: 0)  == 1 || monthReport == monthEndDate) && yearReport == yearEndDate)){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.endDate"))
            }
        }
        // Validate thời gian yêu cầu của các tháng báo cáo phải là liên tiếp

        val monthPre: Int
        val yearPre: Int
        val monthNext: Int
        val yearNext: Int
        // Kiểm tra xem tháng của import vào trường hợp đặc biệt tháng 12 và 1 thì phải sang năm mới

        if(monthReport == 1){
            monthPre = 12
            yearPre = yearReport - 1
        }else {
            monthPre = monthReport!! - 1
            yearPre = yearReport
        }
        if(monthReport == 12){
            monthNext = 1
            yearNext = yearReport + 1
        }else {
            monthNext = monthReport + 1
            yearNext = yearReport
        }
        // check xem có tồn tại dữ liệu của tháng trước không

        val queryTapePre = tapeInfoRep.getTapeDetailByMonth(monthPre,yearPre)
        if(queryTapePre != null){
            val subtraction = request.startDate?.plusHours(7)!!.until(queryTapePre.requestDateEnd, ChronoUnit.DAYS)
            if(subtraction != 1L){
                throw BusinessException(CommonUtils.getMessage("validate.importTape.orderRequestDate"))
            }
        }
        // check xem có tồn tại dữ liệu của tháng sau không

        val queryTapeNext = tapeInfoRep.getTapeDetailByMonth(monthNext,yearNext)
        if(queryTapeNext != null){
            val subtraction = queryTapeNext.requestDateStart!!.until(request.endDate!!.plusHours(7), ChronoUnit.DAYS)
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

        val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/Import_GiaTape_template.xlsx"

        if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 5))
            throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

        val total = sheet.lastRowNum

        // Lấy ra list sản phẩm trong bảng đơn hàng
        val listOrderInfo = orderInfoRep.getProductNameByOder(request.startDate, request.endDate)
        val listProductOrder: MutableList<String> = listOrderInfo.mapNotNull { it?.productName }.distinct().toMutableList()


        // List lưu những sản phẩm validation
        val listOderInfoValidate : MutableList<ImportTapeErrResponse> = mutableListOf()

        // List product trong file import
        val listProductImport : MutableList<String> = mutableListOf()

        // List tỷ lệ đạt theo sản phẩm có thời gian sớm nhất
        val listProductCompletionRate = completionRateProductRepository.getCompletionRateMinProduct()
        // Đọc file import
        var check = true
        val masterData = masterDataService.getMasterDataSelection()
        for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
            val messageErr = ImportTapeErrResponse()
            val productName = ExcelHelper.getCellValue(row, 0)
            val exportType = ExcelHelper.getCellValue(row, 1)
            val tapeShared = ExcelHelper.getCellValue(row, 2)
            val tapeType = ExcelHelper.getCellValue(row, 3)
            val unitPrice = ExcelHelper.getCellValue(row, 4)

            // Validate file import
            if (productName.isEmpty()) {
                check = false
                messageErr.listMessageErr.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 0))
                    )
                )
            }else {
                val checkCompletionRate = listProductCompletionRate.firstOrNull{
                    it?.productName == productName
                }
                if(checkCompletionRate == null){
                    check = false
                    messageErr.listMessageErr.add(
                        CommonUtils.getMessage(
                            "validate.importTape.completionRateProduct1"))
                }else {
                    if(checkCompletionRate.effectiveDate!! > request.startDate){
                        check = false
                        messageErr.listMessageErr.add(
                            CommonUtils.getMessage(
                                "validate.importTape.completionRateProduct2"))
                    }
                }
            }
            if (exportType.isEmpty()) {
                check = false
                messageErr.listMessageErr.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 1))
                    )
                )
            }
            if (tapeShared.isEmpty()) {
                check = false
                messageErr.listMessageErr.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 2))
                    )
                )
            }

            if (tapeShared.isNotEmpty() && !masterData.tapeCommonSelections.any { x -> x.label == ExcelHelper.getCellValue(row, 2) }) {
                check = false
                messageErr.listMessageErr.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 2))))
            }



            if (tapeType.isEmpty()) {
                check = false
                messageErr.listMessageErr.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 3))
                    )
                )
            }
            if (tapeType.isNotEmpty() && !masterData.tapeTypeSelections.any { x -> x.label?.trim() == ExcelHelper.getCellValue(row, 3).trim() }) {
                check = false
                messageErr.listMessageErr.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 3))))
            }

            if (exportType.isNotEmpty() && !masterData.exportTypeSelections.any { x -> x.label?.trim() == ExcelHelper.getCellValue(row, 1).trim() }) {
                check = false
                messageErr.listMessageErr.add(CommonUtils.getMessage("validate.excel.notExist", arrayOf(ExcelHelper.getCellValue(headerRow, 1))))
            }

            if (unitPrice.isEmpty()) {
                check = false
                messageErr.listMessageErr.add(
                    CommonUtils.getMessage(
                        "validate.excel.empty",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 4))
                    )
                )
            }

            if (productName.isNotEmpty() && productName.length > 12){
                check = false
                messageErr.listMessageErr.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 0), 12)))
            }

            if (tapeShared.isNotEmpty() && tapeShared.length > 20){
                check = false
                messageErr.listMessageErr.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 2), 20)))
            }

            if (tapeType.isNotEmpty() && tapeType.length > 20){
                check = false
                messageErr.listMessageErr.add(CommonUtils.getMessage(
                    "validate.excel.maxLength",
                    arrayOf(ExcelHelper.getCellValue(headerRow, 3), 20)))
            }

            if (unitPrice.isNotEmpty() && row.getCell(4).cellType != CellType.NUMERIC){
                check = false
                messageErr.listMessageErr.add(
                    CommonUtils.getMessage(
                        "validate.excel.isNumber",
                        arrayOf(ExcelHelper.getCellValue(headerRow, 4))
                    )
                )
            }

            if(productName.isNotEmpty()){
                val hasProductName = listOrderInfo.firstOrNull{
                    it?.productName == ExcelHelper.getCellValue(row, 0)
                }
                if(hasProductName == null){
                    check = false
                    messageErr.listMessageErr.add(
                        CommonUtils.getMessage(
                            "validate.importTape.productName1",
                            arrayOf(request.startDate!!.plusHours(7).format(DateTimeFormatter.ofPattern("yyyy_MM_dd")),
                                request.endDate!!.plusHours(7).format(DateTimeFormatter.ofPattern("yyyy_MM_dd")))
                        )
                    )
                }
            }



            listProductImport.add(ExcelHelper.getCellValue(row, 0))



            messageErr.productName = productName
            messageErr.tapeShared = tapeShared
            messageErr.typeTape = tapeType
            messageErr.unitPrice = round(unitPrice.toDouble()*1000)/1000
            messageErr.exportTye = exportType
            messageErr.cellStyles = row.map { m -> CellStyleModel(m.columnIndex, m.cellStyle) }

            listOderInfoValidate.add(messageErr)

        }

        // Lấy ra những đơn giá cao nhất của sản phẩm
        val listPriceMax: MutableList<ImportTapeErrResponse> = mutableListOf()
        val listOderInfoValidateGr = listOderInfoValidate.groupBy { it.productName }
        for(item in listOderInfoValidateGr){
            val itemListOderInfoValidateGr  = item.value.sortedBy { it.unitPrice }.last()
            val itemPriceMax = ImportTapeErrResponse(
                unitPrice = itemListOderInfoValidateGr.unitPrice,
                productName = item.key
            )
            listPriceMax.add(itemPriceMax)
        }




        // Kiểm tra những sản phẩm có trong order mà không có trong file import
        for (item in listProductOrder){
            val messageErr = ImportTapeErrResponse()
            val checkProduct = listOderInfoValidate.firstOrNull{
                it.productName == item
            }
            if(checkProduct == null){
                check = false
                messageErr.productName = item
                messageErr.listMessageErr.add(
                    CommonUtils.getMessage(
                        "validate.importTape.productName2",
                        arrayOf(request.startDate!!.plusHours(7).format(DateTimeFormatter.ofPattern("yyyy_MM_dd")),
                            request.endDate!!.plusHours(7).format(DateTimeFormatter.ofPattern("yyyy_MM_dd")))
                    )
                )
                listOderInfoValidate.add(messageErr)
            }
        }


        if(check){
            // Xóa toàn bộ các bản ghi trong tháng nếu tháng đó đã có dữ liệu
            tapeInfoRep.deleteTapeByMonth(request.monthReport, request.yearReport)

            // tính ra giá tiền
            for (item in listOderInfoValidate) {
                val completionRates = completionRateProductRepository.getProductDetail(item.productName)

                var quantityTape = 0
                val productOrder = listOrderInfo.filter { it?.productName == item.productName }
                // Tính ra số lượng của tape
                for (item1 in productOrder) {
                    val rate = completionRates.filter {
                        it?.effectiveDate!! < item1?.orderDate
                    }
                        .sortedByDescending { it?.effectiveDate }
                        .first()
                    quantityTape += if(item1?.blockSh == null || rate?.rate == null || item1.quantity == null
                        || item1.blockSh == 0 || round(rate.rate!!.toDouble()) == 0.0
                    ){
                        0
                    } else {
                        round(((item1.quantity ?: 0) / ((item1.blockSh!!.toDouble() ) * (rate.rate!!.toDouble()) / 100))).toInt()
                    }
                }
                val unitPrice = listPriceMax.firstOrNull { it.productName == item.productName }?.unitPrice ?: 0.0
                val intoMoney = round(quantityTape * unitPrice )
                item.quantityTape = quantityTape
                item.intoMoney = intoMoney
            }

            val requestImport = listOderInfoValidate.map {
                x -> TapeInfo(
                    productName = x.productName,
                    tapeShared = x.tapeShared,
                    typeTape = x.typeTape,
                    quantityTape = x.quantityTape,
                    unitPrice = x.unitPrice,
                    intoMoney = x.intoMoney,
                    monthReport = request.monthReport,
                    yearReport = request.yearReport,
                    requestDateStart = request.startDate,
                    requestDateEnd = request.endDate,
                    exportType = x.exportTye
                )
            }

            tapeInfoRep.bulkInsert(requestImport)
            return  BaseResponse(
                null,
                CommonUtils.getMessage("import.success", arrayOf(total, total))
            )

        }else {
            val excelBytes = exportExcelErr(listOderInfoValidate, headerRow, workbook, sheet)

            val response = FileContentModel(
                fileName = CommonUtils.getMessage("export.excel.result.import", arrayOf(
                    LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
                contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
                content = excelBytes
            )
            workbook.close()
            return BaseResponse(response, message = CommonUtils.getMessage("import.insertNoData"))
        }
    }


    fun exportExcelErr(requestErr: MutableList<ImportTapeErrResponse>, titleRow: Row, workbook: Workbook, importSheet: Sheet): ByteArray? {

        val sheet = workbook.createSheet()
        val headerRow = sheet.getRow(0) ?: sheet.createRow(0)
        headerRow.height = titleRow.height
        for (i in 0 until titleRow.lastCellNum) {
            val headerStyle = titleRow.getCell(i).cellStyle
            val headerCellValue = ExcelHelper.getCellValue(titleRow, i)
            ExcelHelper.setCellValue(headerRow, i, headerStyle, headerCellValue)
            sheet.setColumnWidth(i, importSheet.getColumnWidth(i))
        }

        ExcelHelper.createColResult(headerRow, sheet)

        if (requestErr.isNotEmpty()) {
            val style = requestErr.first().cellStyles.first().cellStyle
            val resultCellStyle = ExcelHelper.getCellStyleResultCol(workbook, style)
            val rowHeight = importSheet.getRow(1).height
            var rowNumber = 1
            for (item in requestErr) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                dataRow.height = rowHeight
                ExcelHelper.setCellValue(dataRow, 0, item.cellStyles.find { x -> x.index == 0 }?.cellStyle ?: style, item.productName)
                ExcelHelper.setCellValue(dataRow, 1, item.cellStyles.find { x -> x.index == 1 }?.cellStyle ?: style, item.exportTye)
                ExcelHelper.setCellValue(dataRow, 2, item.cellStyles.find { x -> x.index == 2 }?.cellStyle ?: style, item.tapeShared)
                ExcelHelper.setCellValue(dataRow, 3, item.cellStyles.find { x -> x.index == 3 }?.cellStyle ?: style, item.typeTape)
                ExcelHelper.setCellValue(dataRow, 4, item.cellStyles.find { x -> x.index == 4 }?.cellStyle ?: style, item.unitPrice.toString())
                ExcelHelper.setCellValue(dataRow, 5, (item.cellStyles.find { x -> x.index == 5 }?.cellStyle ?: resultCellStyle), item.listMessageErr.joinToString(separator = "; "))
            }
        }
        workbook.removeSheetAt(0)
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        return byteArrayOutputStream.toByteArray()
    }

    fun getReportMaterials(request: GetReportMaterialsRequest, pageable: Pageable) : BasePagingResponse<TapeInfo?> {
        val result =  tapeInfoRep.getAllReportMaterials(request, pageable)
        val response = BasePagingResponse<TapeInfo?>()
        response.data = result.first
        response.totalRecords = result.second ?: 0
        return response
    }

    fun checkImportTape(month: Int , year: Int) : CheckImportTapeResponse {
        val data = CheckImportTapeResponse()
        val query = tapeInfoRep.checkImportTape(month, year)
        if(query != null){
            data.hasImportTape = true
            return data
        }
        return data
    }

    fun exportExcel(request: GetReportMaterialsRequest, pageable: Pageable) : BaseResponse<FileContentModel>{
        val query = tapeInfoRep.getAllReportMaterials(request, pageable)
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportTapeTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)

        if (query.first.isNotEmpty()) {
            val style = ExcelHelper.getCellStyleCommon(workbook)
            var rowNumber = 1
            for (item in query.first) {
                val dataRow: Row = sheet.createRow(rowNumber++)
                ExcelHelper.setCellValue(dataRow, 0, style, "${item?.monthReport}/${item?.yearReport}")
                ExcelHelper.setCellValue(dataRow, 1, style, DateTimeHelper.convertOffSetDateTimeUtc7ToString(item?.requestDateStart))
                ExcelHelper.setCellValue(dataRow, 2, style, DateTimeHelper.convertOffSetDateTimeUtc7ToString(item?.requestDateEnd))
                ExcelHelper.setCellValue(dataRow, 3, style, item?.productName)
                ExcelHelper.setCellValue(dataRow, 4, style, item?.exportType)
                ExcelHelper.setCellValue(dataRow, 5, style, item?.tapeShared)
                ExcelHelper.setCellValue(dataRow, 6, style, item?.typeTape)
                ExcelHelper.setCellValue(dataRow, 7, style, item?.unitPrice.toString())
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage("export.tapeInfo.Template", arrayOf(LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }

}