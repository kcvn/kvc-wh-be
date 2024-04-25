package com.kcvn.spm.app.report.externalquality.service

import com.kcvn.spm.app.completionrate.payload.response.CheckImportResponse
import com.kcvn.spm.app.inventoryproduct.payload.response.InventoryProductResponse
import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.service.OrderService
import com.kcvn.spm.app.report.externalquality.payload.model.ExternalQualityDetailExistModel
import com.kcvn.spm.app.report.externalquality.payload.model.ExternalQualityDetailModel
import com.kcvn.spm.app.report.externalquality.payload.model.ExternalQualityReportModel
import com.kcvn.spm.app.report.externalquality.payload.model.KeyValueCustom
import com.kcvn.spm.app.report.externalquality.payload.request.ExternalQualityReportSearchRequest
import com.kcvn.spm.app.report.externalquality.payload.response.ExternalQualityReportResponse
import com.kcvn.spm.common.constants.*
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.helper.NumberHelper
import com.kcvn.spm.common.helper.StringHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.CalendarResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TapeEnRoute
import com.kcvn.spm.model.tables.pojos.TapeInventory
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.repository.*
import org.apache.poi.ss.usermodel.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import org.springframework.web.multipart.MultipartFile
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter


@Service
@Transactional
class ExternalQualityReportService(
    private val orderSer: OrderService,
    private val workResultRep: WorkResultRepository,
    private val inventoryProductRep: InventoryProductRepository,
    private val completionRateProductRep: CompletionRateProductRepository,
    private val holidaysCalenderRep: HolidaysCalenderRepository,
    private val orderInfoRepository: OrderInfoRepository,
    private val tapeEnRouteRepository:TapeEnRouteRepository,
    private val tapeInventoryRepository: TapeInventoryRepository,
    private val appSettingRep: AppSettingRepository,
    private val productRepository: ProductRepository
) {
    //region IMPORT

    fun checkImportTapeEnRouteExcel(couponCode: String): BaseResponse<CheckImportResponse> {
        val checkTape = tapeEnRouteRepository.getTapeEnRouteList(couponCode)
        return if(checkTape.isNotEmpty()){
            BaseResponse(CheckImportResponse(false,CommonUtils.getMessage("validate.importTapeRoute", arrayOf(couponCode))), "")
        } else{
            BaseResponse(CheckImportResponse(true,""), "")
        }
    }
    fun checkImportTapeInventory(stocktakingDay: OffsetDateTime): BaseResponse<CheckImportResponse>{
        val stocktakingDayConvert = DateTimeHelper.toTimeZone7(stocktakingDay)
        val checkTape = tapeInventoryRepository.getTapeEnRouteListByDate(stocktakingDayConvert)
        return if(checkTape.isNotEmpty()){
            BaseResponse(CheckImportResponse(false,CommonUtils.getMessage("validate.importTapeInventory", arrayOf(stocktakingDayConvert!!.toLocalDate()))), "")
        } else{
            BaseResponse(CheckImportResponse(true,""), "")
        }
    }
    fun downloadTapeEnRouteTemplate(): BaseResponse<FileContentModel>{
        val fileUrl = "ImportTapeEnRouteTemplate.xlsx"
        val fileName = "fileName.importTapeEnRouteTemplate"
        return downloadTemplate(fileUrl, fileName)
    }
    fun downloadTapeInventoryTemplate(): BaseResponse<FileContentModel>{
        val fileUrl = "ImportTapeInventoryTemplate.xlsx"
        val fileName = "fileName.importTapeInventoryTemplate"
        return downloadTemplate(fileUrl, fileName)
    }
    fun downloadTemplate(fileUrl:String, fileName: String): BaseResponse<FileContentModel> {
        val filePath = "${System.getProperty("user.dir")}/target/classes/assets/template/"+fileUrl
        val workbook = FileInputStream(filePath).use { x -> XSSFWorkbook(x) }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage(fileName),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return BaseResponse(response)
    }
    fun importTapeEnRoute(file: MultipartFile, couponCode: String): BaseResponse<FileContentModel> {
        val tapeEnRoutes = tapeEnRouteRepository.getTapeEnRouteList(couponCode)
        if(tapeEnRoutes.isNotEmpty()){
            tapeEnRouteRepository.deleteTapeEnRouteList(tapeEnRoutes)
        }
        val workbook = WorkbookFactory.create(file.inputStream)
        try {
            val productMaster = productRepository.getProductList()
            val dateFormats = listOf(DateTimeFormat.M_dd_yyyy, DateTimeFormat.yyyy_MM_dd, DateTimeFormat.dd_MM_yyyy)

            val tapeEnRouteList = mutableListOf<TapeEnRoute>()
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1

            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            val headerRow = sheet.getRow(0)
                ?: throw BusinessException(CommonUtils.getMessage("validate.excel.headerInFirstRow"))
            val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportTapeEnRouteTemplate.xlsx"
            val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)
            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 16))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))

            var count = 0
            var total = 0
            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val style = row.getCell(0)?.cellStyle ?: break
                val messageResults = mutableListOf<String>()
                var isValidCol = true

                val monthOrderCheck = ExcelHelper.getCellValue(row, 0)
                val monthOrder: Int? = if(monthOrderCheck.isNotEmpty()) {
                    StringHelper.removeDecimalSuffix(monthOrderCheck).toInt()
                } else {
                    null
                }

                var exportType = ExcelHelper.getCellValue(row, 1)
                if(exportType.isEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.exportType"))
                }else if(exportType == ExportType.SIPBACK){
                    exportType = ExportType.SHIPBACK
                }
                if (exportType != ExportType.SHIPBACK && exportType != ExportType.DIRECT && exportType.isNotEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.format.exportType"))
                }

                val supplierCd= ExcelHelper.getCellValue(row, 2)

                val purchaseOrder = ExcelHelper.getCellValue(row, 3)
                if(purchaseOrder.isEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.purchaseOrder"))
                }
                val itemCd = ExcelHelper.getCellValue(row, 4)
                if(itemCd.isEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.itemCode"))
                }

                val description = ExcelHelper.getCellValue(row, 5)

                val spec = ExcelHelper.getCellValue(row, 6)
                if(spec.isEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.spec"))
                }
                if(spec.length == 15){
                    val productNameShortCut = spec.substring(1, 8)
                    val line1 = spec.substring(12, 13)
                    val frame1 = spec.substring(13, 15)
                    val productExist = productMaster.find { x-> x.name?.contains(productNameShortCut) == true && x.exportType?.contains(exportType) ==true && x.frame_1==frame1 && x.layerCount== line1.toInt()}
                    if(productExist == null){
                        isValidCol = false
                        messageResults.add(CommonUtils.getMessage("validate.spec.productNotExist"))
                    }
                }else{
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.wrongFormat.spec"))
                }

                val orderedQuantityCheck = ExcelHelper.getCellValue(row, 7)
                var orderedQuantity: Int? = null
                if (!NumberHelper.isNumeric(orderedQuantityCheck)) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.orderedQuantity.format"))
                } else {

                    orderedQuantity = StringHelper.removeDecimalSuffix(orderedQuantityCheck).toInt()
                }

                val qtyUm = ExcelHelper.getCellValue(row, 8)
                val opuPFc = ExcelHelper.getCellValue(row, 9)
                val opuP = ExcelHelper.getCellValue(row, 10)
                val opDlvDtCheck = ExcelHelper.getCellValue(row, 11,DateTimeFormat.M_dd_yyyy)

                val isDateFormatOpDlvDt = DateTimeHelper.isDateFormatDateCustom(opDlvDtCheck)

                val opDlvDt = if (isDateFormatOpDlvDt) {
                     DateTimeHelper.convertStringToOffSetDateTime(opDlvDtCheck, dateFormats)
                } else {
                    null
                }

                val transmit = ExcelHelper.getCellValue(row, 12)

                val deliveredQuantityCheck = ExcelHelper.getCellValue(row, 13)
                var deliveredQuantity: Int? = null
                if (deliveredQuantityCheck.isEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.deliveredQuantity"))
                } else if (!NumberHelper.isNumeric(deliveredQuantityCheck)) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.deliveredQuantity.format"))
                } else {
                    deliveredQuantity = StringHelper.removeDecimalSuffix(deliveredQuantityCheck).toInt()
                }

                val responseDateCheck = ExcelHelper.getCellValue(row, 14, DateTimeFormat.M_dd_yyyy)
                val isDateFormat = DateTimeHelper.isDateFormatDateCustom(responseDateCheck)

                if (!isDateFormat && responseDateCheck.isNotEmpty()) {
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.responseDate.format"))
                }

                val responseDate = if(responseDateCheck.isNotEmpty() && isDateFormat) DateTimeHelper.convertStringToOffSetDateTime(responseDateCheck,dateFormats) else null

                if(responseDateCheck.isEmpty()){
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.responseDate"))
                }

                val estimatedDateCheck = ExcelHelper.getCellValue(row, 15, DateTimeFormat.M_dd_yyyy)
                val isDateFormatEstimatedDate = DateTimeHelper.isFormatDate(estimatedDateCheck, DateTimeFormat.M_dd_yyyy)
                val estimatedDate = if (!isDateFormatEstimatedDate) {
                    DateTimeHelper.convertStringToOffSetDateTime(estimatedDateCheck, dateFormats)
                } else {
                    null
                }

                val estimatedMonthCheck =  ExcelHelper.getCellValue(row, 16)
                val estimatedMonth = if(estimatedMonthCheck.isNotEmpty()) {
                    StringHelper.removeDecimalSuffix(estimatedMonthCheck).toInt()
                } else {
                    null
                }
                if (purchaseOrder.isEmpty() && itemCd.isEmpty() && spec.isEmpty() && orderedQuantityCheck.isEmpty() && responseDateCheck.isEmpty() && estimatedMonthCheck.isEmpty() && deliveredQuantityCheck.isEmpty() && opDlvDtCheck.isEmpty() && transmit.isEmpty() && estimatedDateCheck.isEmpty() && qtyUm.isEmpty() && opuPFc.isEmpty() && opuP.isEmpty()) {
                    continue
                }

                total++

                val tapeEnRoute = TapeEnRoute(
                    orderPlacementMonth = monthOrder,
                    exportType = exportType,
                    supplierCd = supplierCd,
                    purchaseOrder = purchaseOrder,
                    itemCd = itemCd,
                    description = description,
                    spec = spec,
                    orderedQuantity = orderedQuantity,
                    qtyUm = qtyUm,
                    opuPFc = opuPFc,
                    opuP = opuP,
                    opDlvDt = opDlvDt,
                    transmit = transmit,
                    deliveredQuantity = deliveredQuantity,
                    responseDate = responseDate,
                    estimatedDate = estimatedDate,
                    estimatedMonth = estimatedMonth,
                    couponCode = couponCode
                )
                if (isValidCol) {
                    tapeEnRouteList.add(tapeEnRoute)
                    count++
                }
                val result = messageResults.joinToString(separator = "; ")

                if (row.getCell(colIndexResult) == null) {
                    row.createCell(colIndexResult)
                }
                row.getCell(colIndexResult).setCellValue(result)
                row.getCell(colIndexResult).cellStyle = ExcelHelper.getCellStyleResultCol(workbook, style)
            }
            if (count == total) {
                for(tapeEnRoute in tapeEnRouteList){
                    tapeEnRouteRepository.addTapeEnRoute(tapeEnRoute)
                }
                return BaseResponse(null, CommonUtils.getMessage("import.success", arrayOf(count, total)))
            }
            val errorRows = sheet.filter { x -> ExcelHelper.getCellValue(x, colIndexResult) != CommonUtils.getMessage("validate.excel.importSuccess") }
            val response = exportFileError(errorRows, workbook, sheet, "fileName.importTapeEnRouteTemplate")
            workbook.close()
            return BaseResponse(
                response,
                if (count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("importTape.success", arrayOf(count, total))
            )
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }

    fun importTapeInventory(file: MultipartFile, stocktakingDay: OffsetDateTime): BaseResponse<FileContentModel> {

        val stocktakingDayConvert = DateTimeHelper.toTimeZone7(stocktakingDay)
        val tapeInventoriesCheck = tapeInventoryRepository.getTapeEnRouteListByDate(stocktakingDayConvert)
        if(tapeInventoriesCheck.isNotEmpty()){
            tapeInventoryRepository.deleteTapeInventoryList(tapeInventoriesCheck)
        }
        val workbook = WorkbookFactory.create(file.inputStream)
        try {
            val tapeInventories = mutableListOf<TapeInventory>()
            val sheet = workbook.getSheetAt(0)
            val rowIndex = 1

            if (!sheet.any { x -> x.rowNum >= rowIndex } || ExcelHelper.fileIsEmpty(sheet, rowIndex))
                throw BusinessException(CommonUtils.getMessage("import.file.empty"))
            val headerRow = sheet.getRow(0)
                ?: throw BusinessException(CommonUtils.getMessage("validate.excel.headerInFirstRow"))
            val templateUrl = "${System.getProperty("user.dir")}/target/classes/assets/template/ImportTapeInventoryTemplate.xlsx"
            val colIndexResult = ExcelHelper.createColResult(headerRow, sheet)
            if (!ExcelHelper.columnIsMatchingTemplate(templateUrl, headerRow, 0, 6))
                throw BusinessException(CommonUtils.getMessage("validate.excel.invalidFormat"))
            val productMaster = productRepository.getProductList()

            var count = 0
            var total = 0
            for (row in sheet.filter { x -> x.rowNum >= rowIndex }) {
                val style = row.getCell(0)?.cellStyle ?: break
                val messageResults = mutableListOf<String>()
                var isValidCol = true
                var exportType = ExcelHelper.getCellValue(row, 0)
                if(exportType.isEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeRoute.exportType"))
                }else if(exportType == ExportType.SIPBACK){
                    exportType = ExportType.SHIPBACK
                }
                if (exportType != ExportType.SHIPBACK && exportType != ExportType.DIRECT && exportType.isNotEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.format.exportType"))
                }

                val productNameShortCut = ExcelHelper.getCellValue(row, 1)
                if (productNameShortCut.isEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeInventory.productNameShortCut"))
                }


                val productName = ExcelHelper.getCellValue(row, 2)
                if (productName.isEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeInventory.productName"))
                }


                if(productMaster.none { product -> product.name == productName }){
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeInventory.productName.notExist"))
                }

                if(productMaster.none { product -> product.name == productName && product.exportType?.contains(exportType) == true }){
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.spec.productNotExist"))
                }
                val tapeInWareHouseCheck = ExcelHelper.getCellValue(row, 3)
                var tapeInWareHouse: Int? = null
                 if (!NumberHelper.isNumeric(tapeInWareHouseCheck) && tapeInWareHouseCheck.isNotEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeInventory.tapeInWareHouse.format"))
                } else {
                    if(tapeInWareHouseCheck.isNotEmpty())
                    tapeInWareHouse = StringHelper.removeDecimalSuffix(tapeInWareHouseCheck).toInt()
                }

                val tapeInDepartmentCheck = ExcelHelper.getCellValue(row, 4)
                var tapeInDepartment: Int? = null
                if (!NumberHelper.isNumeric(tapeInDepartmentCheck) && tapeInDepartmentCheck.isNotEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeInventory.tapeInDepartment.format"))
                } else {
                    if(tapeInDepartmentCheck.isNotEmpty())
                    tapeInDepartment = StringHelper.removeDecimalSuffix(tapeInDepartmentCheck).toInt()
                }

                val tapeNGCheck = ExcelHelper.getCellValue(row, 5)
                var tapeNG: Int? = null
                 if (!NumberHelper.isNumeric(tapeNGCheck) && tapeNGCheck.isNotEmpty()) {
                    isValidCol = false
                    messageResults.add(CommonUtils.getMessage("validate.importTapeInventory.tapeNG.format"))
                } else {
                    if(tapeNGCheck.isNotEmpty())
                    tapeNG = StringHelper.removeDecimalSuffix(tapeNGCheck).toInt()
                }
                if (exportType.isEmpty()  && productName.isEmpty() && productNameShortCut.isEmpty() && tapeInWareHouseCheck.isEmpty() && tapeInDepartmentCheck.isEmpty() && tapeNGCheck.isEmpty()){
                    continue
                }
                total++


                val tapeInventory = TapeInventory(
                    exportType = exportType,
                    productNameShortCut = productNameShortCut,
                    productName = productName,
                    tapeInWarehouse = tapeInWareHouse,
                    tapeInDepartment = tapeInDepartment,
                    tapeNg = tapeNG,
                    stocktakingDay = stocktakingDayConvert
                )
                if (isValidCol) {
                    tapeInventories.add(tapeInventory)
                    count++
                }
                val result = messageResults.joinToString(separator = "; ")

                if (row.getCell(colIndexResult) == null) {
                    row.createCell(colIndexResult)
                }
                row.getCell(colIndexResult).setCellValue(result)
                row.getCell(colIndexResult).cellStyle = ExcelHelper.getCellStyleResultCol(workbook, style)
            }
            if (count == total) {
                for(tapeInventory in tapeInventories){
                    tapeInventoryRepository.add(tapeInventory)
                }
                return BaseResponse(null, CommonUtils.getMessage("import.success", arrayOf(count, total)))
            }
            val errorRows = sheet.filter { x -> ExcelHelper.getCellValue(x, colIndexResult) != CommonUtils.getMessage("validate.excel.importSuccess") }
            val response = exportFileError(errorRows, workbook, sheet, "fileName.importTapeInventoryTemplate")
            workbook.close()
            return BaseResponse(
                response,
                if (count == 0) CommonUtils.getMessage("import.insertNoData") else CommonUtils.getMessage("importTape.success", arrayOf(count, total))
            )
        } catch (e: Exception) {
            throw e
        } finally {
            workbook.close()
        }
    }

    private fun exportFileError(dataRows: List<Row>, workbook: Workbook, importSheet: Sheet, fileName: String): FileContentModel {
        val sheet = workbook.createSheet()
        for ((rowNumber, dataRow) in dataRows.withIndex()) {
            val row = sheet.createRow(rowNumber)
            row.height = dataRow.height
            for (colIndex in 0 until dataRow.lastCellNum) {
                if (rowNumber == 0) {
                    sheet.setColumnWidth(colIndex, importSheet.getColumnWidth(colIndex))
                }
                val cell = dataRow.getCell(colIndex)
                if(cell !=null){
                    val style = cell.cellStyle
                        val value = ExcelHelper.getCellValueCustom(dataRow, colIndex)
                        ExcelHelper.setCellValue(row, colIndex, style, value)
                }
            }
        }
        workbook.removeSheetAt(0)
        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)

        val excelBytes = byteArrayOutputStream.toByteArray()

        val response = FileContentModel(
            fileName = CommonUtils.getMessage(CommonUtils.getMessage(fileName), arrayOf(
                LocalDateTime.now().format(
                    DateTimeFormatter.ofPattern("yyyy_MM_dd_HH_mm_ss")))),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        return response
    }
    //endregion
    //region EXPORT
    fun exportExcelExternalQualityReport(request: ExternalQualityReportSearchRequest, pageable: Pageable): BaseResponse<FileContentModel> {
        val dataExport = getDataReport(request,pageable)
        val fileTemplate = File("${System.getProperty("user.dir")}/target/classes/assets/template/ExportExternalQualityReportTemplate.xlsx")
        val workbook = FileInputStream(fileTemplate).use { x -> XSSFWorkbook(x) }
        val sheet = workbook.getSheetAt(0)
        val headerRow = sheet.getRow(0)
        val secondRow = sheet.getRow(1)
        var headerCol = 14
        val style = headerRow.getCell(0).cellStyle
        // create subColumns 14 columns from 1
        if(dataExport.subColumns.isNotEmpty()){
            for (col in dataExport.subColumns) {
                ExcelHelper.setCellValueWithCalendar(workbook, headerRow, headerCol, style, col.value, false)
                headerCol++
            }
            headerCol= 14
        }
        //create columns
        if(dataExport.columns.isNotEmpty()){
            for (col in dataExport.columns) {
                ExcelHelper.setCellValueWithCalendar(workbook, secondRow, headerCol, style, col.value, false)
                headerCol++
            }
        }
        //create data filter
        val rowNumber = 2
        var numberRowData = 12

        if (dataExport.data?.isNotEmpty() == true) {
            var rowProductIndex = rowNumber
            var rowShippingIndex = rowNumber
            var rowReportIndex =rowNumber
            for (productReport in dataExport.data!!) {
                if(productReport.exportType?.contains(",") == true) {
                    numberRowData = 18
                }
                for (i in 0 until numberRowData) {
                    val dataRow = sheet.getRow(rowProductIndex) ?: sheet.createRow(rowProductIndex)
                    ExcelHelper.run {
                        setCellValueCustom(workbook,dataRow, 0, style, productReport.productShortcutName, isAlignCenter = true)
                        setCellValueCustom(workbook,dataRow, 1, style, productReport.productName, isAlignCenter = true)
                        setCellValueCustom(workbook,dataRow, 2, style, productReport.mold, isAlignCenter = true)
                        setCellValueCustom(workbook,dataRow, 3, style, productReport.pcsSh.toString(), isAlignCenter = true)
                        setCellValueCustom(workbook,dataRow, 4, style, productReport.blockSh.toString(), isAlignCenter = true)
                        setCellValueCustom(workbook,dataRow, 5, style, productReport.productLine, isAlignCenter = true)
                        setCellValueCustom(workbook,dataRow, 6, style, productReport.snapMold, isAlignCenter = true)
                        setCellValueCustom(workbook,dataRow, 7, style, productReport.layerCount.toString(), isAlignCenter = true)
                        setCellValueCustom(workbook,dataRow, 8, style, productReport.tapeCommon, isAlignCenter = true)
                        val completionRate = if (productReport.completionRate?.toString().isNullOrEmpty()) "" else "${productReport.completionRate}%"
                        setCellValueCustom(workbook, dataRow, 9, style, completionRate, isAlignCenter = true)
                    }
                    rowProductIndex++
                }
                numberRowData = 12
            }
            for (productReport in dataExport.data!!) {
                for (shippingData in productReport.shippingData) {
                    val dataRow = sheet.getRow(rowShippingIndex) ?: sheet.createRow(rowShippingIndex)
                    ExcelHelper.setCellValueCustom(workbook,dataRow, 11, style, shippingData.value, isAlignCenter = true)
                    rowShippingIndex++
                }
            }
            rowShippingIndex = 2
            for (productReport in dataExport.data!!) {
                for (i in 0 until 6) {
                    val dataRow = sheet.getRow(rowShippingIndex++) ?: sheet.createRow(rowShippingIndex++)
                    ExcelHelper.setCellValueCustom(workbook,dataRow, 10, style, "", isAlignCenter = true)
                }
               if(productReport.exportType1 !=null){
                   for (i in 0 until 6) {
                       val dataRow = sheet.getRow(rowShippingIndex++) ?: sheet.createRow(rowShippingIndex++)
                       ExcelHelper.setCellValueCustom(workbook,dataRow, 10, style, productReport.exportType1, isAlignCenter = true)
                   }
               }
                if(productReport.exportType2 !=null){
                    for (i in 0 until 6) {
                        val dataRow = sheet.getRow(rowShippingIndex++) ?: sheet.createRow(rowShippingIndex++)
                        ExcelHelper.setCellValueCustom(workbook,dataRow, 10, style, productReport.exportType2, isAlignCenter = true)
                    }
                }


            }

            for (productReport in dataExport.data!!) {
                for (reportData in productReport.details) {
                    val dataRow = sheet.getRow(rowReportIndex) ?: sheet.createRow(rowReportIndex)
                    ExcelHelper.setCellValueCustom(workbook,dataRow, 12, style, reportData.title)
                    ExcelHelper.setCellValueCustom(workbook,dataRow, 13, style, reportData.inventory?.toString() ?: "")
                    headerCol=14
                    for(col in reportData.quantityByCalendars){
                        ExcelHelper.setCellValueWithCalendar(workbook, dataRow, headerCol, style, col.value, false,isReportDetails = true)
                        headerCol++
                    }

                    rowReportIndex++
                }
            }
        }

        val byteArrayOutputStream = ByteArrayOutputStream()
        workbook.write(byteArrayOutputStream)
        val excelBytes = byteArrayOutputStream.toByteArray()
        val response = FileContentModel(
            fileName = CommonUtils.getMessage("fileName.exportReportExternalQuality",
                        arrayOf((DateTimeHelper.toTimeZone7(request.startDate))?.toLocalDate().toString(),(DateTimeHelper.toTimeZone7(request.endDate))?.toLocalDate().toString())),
            contentType = ExcelConstant.EXCEL_CONTENT_TYPE,
            content = excelBytes
        )

        workbook.close()

        return BaseResponse(response)
    }
    //endregion

    //region GET LIST

    fun getDataReport(request: ExternalQualityReportSearchRequest, pageable: Pageable): ExternalQualityReportResponse {
        if(request.inventoryClosingDate == null){
            return  ExternalQualityReportResponse()
        }
        val inventoryClosingDate = DateTimeHelper.toTimeZone7(request.inventoryClosingDate)
        val startDate = DateTimeHelper.toTimeZone7(request.startDate)
        val endDate = DateTimeHelper.toTimeZone7(request.endDate)
        val response = ExternalQualityReportResponse()
        val orderInfo = orderInfoRepository.getListOrderForReport(request,pageable)
        val mappingPaging = orderInfo.first
        val listProductName = mappingPaging.map { it.productName }
        response.totalRecords = orderInfo.second

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        response.columns = DateTimeHelper.toCalendarColumn(startDate!!, endDate!!, holidayCalenders)
        var daysToSubtract: Long = 10
        val dayToSubtractConfig = appSettingRep.findByKey(TapeReportConfig.KEY)
        if (dayToSubtractConfig != null) {
            daysToSubtract = dayToSubtractConfig.value?.toLong() ?: 10
        }
        val subColumns = DateTimeHelper.toCalendarColumn(startDate, endDate, holidayCalenders, daysToSubtract)
        response.subColumns = subColumns
        val orderSearchRequest = OrderSearchRequest()
        orderSearchRequest.endDate = request.endDate
        orderSearchRequest.startDate = request.startDate
        orderSearchRequest.productName = request.productName
        orderSearchRequest.version = OrderVersion.LATEST

        val pageableOrder: Pageable = PageRequest.of(PagingDefault.PAGE, PagingDefault.EXPORT_SIZE)
        val order = orderSer.getPaginatedOrder(orderSearchRequest,pageableOrder)
        val listProductOrder = order.data

        val listCompletionRate = completionRateProductRep.getForReport(listProductName.filterNotNull(),startDate)
        val listWorkResult = request.startDate?.let { request.endDate?.let { it1 -> workResultRep.getForReport(it, it1,listProductName) } }
        for(externalQuality in mappingPaging){
            if (listCompletionRate != null) {
                externalQuality.completionRate = listCompletionRate.find { x-> x.productName.equals(externalQuality.productName) }?.rate
            }
        }
        //get list name UpdateTape
        val listShortCutName = getListNameProduct(mappingPaging,isShortCutName = true)
        val listSpec = getListNameProduct(mappingPaging,isSpec = true)
        //get list Tape Inventory
        val tapeInventory = tapeInventoryRepository.getTapeForReport(listShortCutName,inventoryClosingDate)
        //get list Tape En route
        val tapeEnRoute = tapeEnRouteRepository.getTapeEnRouteForReport(listSpec,startDate,endDate)

        // get list name product
        val productNames = getListNameProduct(mappingPaging)
        // get list Inventory
        val inventoryDetails = inventoryProductRep.getInventoryProductByProductName(productNames,endDate)
        //create list data exist
        val listDataExist :MutableList<ExternalQualityDetailExistModel> = mutableListOf()
        //add Details Data here
        for(mappingItem in mappingPaging){
            addDetailsExternalQualityReport(mappingItem,listProductOrder,response.columns,listWorkResult,tapeInventory,inventoryDetails, subColumns,listDataExist,tapeEnRoute,daysToSubtract)
        }
        val valueReportDate= request.endDate?.let { DateTimeHelper.toString(it, DateTimeFormat.yyyyMMdd) }
        //add Shipping Data here
        for(mappingItem in mappingPaging){
            addShippingData(mappingItem,valueReportDate,inventoryClosingDate)
        }
        for(mappingItem in mappingPaging){
            addExportType(mappingItem)
        }
        response.data = mappingPaging
        return response
    }

    fun getInventoryProduct(productName: String, inventoryProducts:  List<InventoryProductResponse>): Int{
        val data = inventoryProducts.filter { x-> x.productName == productName }
        return data.sumOf { x -> x.productQuantity!! }
    }

    fun parseExportTypes(exportType: String?): List<String> {
        if (exportType != null) {
            return if (exportType.contains(",")) {
                exportType.split(",").map { it.trim() }
            } else {
                listOf(exportType.trim())
            }
        }
        return listOf()
    }

    fun getListNameProduct(listExternalQuantityReport:  List<ExternalQualityReportModel>, isShortCutName: Boolean =false, isSpec: Boolean = false) :List<String>{

        val productNames: MutableList<String> = mutableListOf()

        for(item in listExternalQuantityReport){
            if(isShortCutName){
                item.productShortcutName?.let { productNames.add(it) }
            }else if(isSpec){
                item.productShortcutName?.let { productNames.add("V$it"+"00 L1") }
            }
            else{
                item.productName?.let { productNames.add(it) }
            }
        }
        return productNames
    }

    fun addDetailsExternalQualityReport(externalQualityReportModel: ExternalQualityReportModel,
                                        listProductOrder:  List<OrderDetailModel>?,
                                        columns:  List<CalendarResponse>,
                                        listWorkResult: List<WorkResult>?,
                                        listTapeInventory:List<TapeInventory>,
                                        inventoryProducts:  List<InventoryProductResponse>,
                                        subColumns: List<CalendarResponse>,
                                        listDataExist: MutableList<ExternalQualityDetailExistModel> = mutableListOf(),
                                        listTapeEnRoute: List<TapeEnRoute>,
                                        daysToSubtract: Long){

        val detailData : MutableList<ExternalQualityDetailModel> = mutableListOf()

        //ORDER QUANTITY
        val detailOrderQuantity =  ExternalQualityDetailModel("ORDER_QUANTITY", ExternalReportDetailType.ORDER_QUANTITY)
        val orderQuantityCalendarsSave =listProductOrder?.firstOrNull { x -> x.productName == externalQualityReportModel.productName }?.quantityByCalendars?.toMutableList()
        val orderQuantityCalendars: MutableList<KeyValueResponse> = mutableListOf()

            columns.forEach { (key) ->
                val existingEntry = orderQuantityCalendarsSave?.find { it.key == key }
                if (existingEntry == null) { orderQuantityCalendars.add(KeyValueResponse(key, "0")) }
                else{
                    val valueEntry = (existingEntry.value)?.toDouble()
                    orderQuantityCalendars.add(KeyValueResponse(key, value= valueEntry?.toInt().toString()))
                }}

            detailOrderQuantity.quantityByCalendars= orderQuantityCalendars

        detailData.add(detailOrderQuantity)

        //ACCUMULATED_ORDER_QUANTITY
        val accumulatedOrderQuantity = ExternalQualityDetailModel("ACCUMULATED_ORDER_QUANTITY", ExternalReportDetailType.ACCUMULATED_ORDER_QUANTITY,externalQualityReportModel.sumInventoryQuantity)
        val accumulatedOrderQuantityCalendars = calculateAccumulation(orderQuantityCalendars)
        accumulatedOrderQuantity.quantityByCalendars = accumulatedOrderQuantityCalendars
        externalQualityReportModel.sumInventoryQuantity = externalQualityReportModel.productName?.let { getInventoryProduct(it,inventoryProducts) }

        accumulatedOrderQuantity.inventory =   externalQualityReportModel.sumInventoryQuantity
        detailData.add(accumulatedOrderQuantity)

        //PRODUCTION_RESULT
        val productionResult = ExternalQualityDetailModel("PRODUCTION_RESULT", ExternalReportDetailType.PRODUCTION_RESULT)
        val productionResultCalendars: MutableList<KeyValueResponse> = mutableListOf()
        columns.forEach{ x ->
        val productWorkResult = listWorkResult?.filter { workResult -> x.key.equals(workResult.summaryResultDate?.let { DateTimeHelper.toString(it, DateTimeFormat.yyyyMMdd) })
                                                        && workResult.itemName == externalQualityReportModel.productName }
        val sumItemQuantity = productWorkResult?.map { it.goodTapeQuantity }?.sumOf { it ?: 0 } ?: 0
        productionResultCalendars.add(KeyValueResponse(x.key, sumItemQuantity.toString())) }

        productionResult.quantityByCalendars = productionResultCalendars
        detailData.add(productionResult)

        //ACCUMULATED_PRODUCTION_RESULT
        val accumulatedProductionResult = ExternalQualityDetailModel("ACCUMULATED_PRODUCTION_RESULT", ExternalReportDetailType.ACCUMULATED_PRODUCTION_RESULT)
        val accumulatedProductionResultCalendars = calculateAccumulation(productionResultCalendars)
        accumulatedProductionResult.quantityByCalendars = accumulatedProductionResultCalendars
        detailData.add(accumulatedProductionResult)

        //DIFFERENCE_1
        val difference1 = ExternalQualityDetailModel("DIFFERENCE_1", ExternalReportDetailType.DIFFERENCE_1)
        difference1.quantityByCalendars = calculateDifferenceCalendars(accumulatedProductionResultCalendars,accumulatedOrderQuantityCalendars)
        detailData.add(difference1)

        //DIFFERENCE_2
        val difference2 = ExternalQualityDetailModel("DIFFERENCE_2", ExternalReportDetailType.DIFFERENCE_2)
        difference2.quantityByCalendars = calculateAccumulationWithInventory(orderQuantityCalendars,externalQualityReportModel.sumInventoryQuantity)
        detailData.add(difference2)

        // LOGIC TAPE

        val exportTypes = parseExportTypes(externalQualityReportModel.exportType)

        var isFirstExportType = true
        for(exportType in exportTypes){
        val productNameShortCut = externalQualityReportModel.productShortcutName
        val tapeInventory = listTapeInventory.filter { x-> x.productNameShortCut == productNameShortCut && x.exportType == exportType}
        var inventoryTape = 0
        var tapeExpired =0
        if(tapeInventory.isNotEmpty()){
            inventoryTape = tapeInventory.filter { it.tapeInWarehouse != null }.sumOf { it.tapeInWarehouse!! } + tapeInventory.filter { it.tapeInDepartment != null }.sumOf { it.tapeInDepartment!! }
            tapeExpired = tapeInventory.filter { it.tapeNg != null }.sumOf { it.tapeNg!! }
        }
        val goodQualityTapeInventorySet = inventoryTape - tapeExpired
        val goodQualityTapeInventoryBlock = goodQualityTapeInventorySet * externalQualityReportModel.blockSh!!
        if(isFirstExportType){
            externalQualityReportModel.tapeInventoryQuantity1 = inventoryTape
            externalQualityReportModel.tapeExpireQuantity1 = tapeExpired
            externalQualityReportModel.exportType1 = exportType
            isFirstExportType = false
        }else{
            externalQualityReportModel.tapeInventoryQuantity2 = inventoryTape
            externalQualityReportModel.tapeExpireQuantity2 = tapeExpired
            externalQualityReportModel.exportType2 = exportType
        }

        externalQualityReportModel.goodQualityTapeInventorySet = goodQualityTapeInventorySet
        externalQualityReportModel.goodQualityTapeInventoryBlock = goodQualityTapeInventoryBlock
        externalQualityReportModel.sumInventoryQuantity =
            externalQualityReportModel.productName?.let { getInventoryProduct(it,inventoryProducts) }

        //PLANNED_TAPE_SET
        val plannedTapeSet = ExternalQualityDetailModel("PLANNED_TAPE_SET", ExternalReportDetailType.PLANNED_TAPE_SET, externalQualityReportModel.goodQualityTapeInventorySet)
        val plannedTapeSetCalendars: MutableList<KeyValueResponse> = mutableListOf()
        val tapeEnRoute = listTapeEnRoute.filter { x ->
            x.exportType == exportType && x.spec?.contains(externalQualityReportModel.productShortcutName ?: "") == true}

        subColumns.forEach{ x ->
            val tape = tapeEnRoute.filter{ tape -> tape.responseDate?.let { DateTimeHelper.toTimeZone7((it.plusDays(daysToSubtract)))
                ?.let { it1 -> DateTimeHelper.toString(it1, DateTimeFormat.yyyyMMdd) } } == x.key }
            if(tape.isNotEmpty()){
                plannedTapeSetCalendars.add(KeyValueResponse(x.key, tape.sumOf { it.deliveredQuantity ?: 0 }.toString()))
            }else{
                plannedTapeSetCalendars.add(KeyValueResponse(x.key, "0"))
            }
        }
        plannedTapeSet.quantityByCalendars =plannedTapeSetCalendars
        detailData.add(plannedTapeSet)

        //ACCUMULATED_PLANNED_TAPE_SET
        val accumulatedPlannedTapeSet = ExternalQualityDetailModel("ACCUMULATED_PLANNED_TAPE_SET", ExternalReportDetailType.ACCUMULATED_PLANNED_TAPE_SET)
        accumulatedPlannedTapeSet.quantityByCalendars = calculateAccumulation(plannedTapeSet.quantityByCalendars,goodQualityTapeInventorySet)
        detailData.add(accumulatedPlannedTapeSet)

        //PLANNED_TAPE_BLOCK
        val plannedTapeBlock = ExternalQualityDetailModel("PLANNED_TAPE_BLOCK", ExternalReportDetailType.PLANNED_TAPE_BLOCK,externalQualityReportModel.goodQualityTapeInventoryBlock)
        val plannedTapeBlockCalendars: MutableList<KeyValueResponse> = mutableListOf()
        val blockShProduct = externalQualityReportModel.blockSh
        accumulatedPlannedTapeSet.quantityByCalendars.forEach{ x ->

            val value = if (blockShProduct != null) {
                x.value?.toInt()?.times(blockShProduct).toString()
            } else {
                "0"
            }
            plannedTapeBlockCalendars.add(KeyValueResponse(x.key, value))

        }
        plannedTapeBlock.quantityByCalendars = plannedTapeBlockCalendars
        detailData.add(plannedTapeBlock)

        //ACCUMULATED_PLANNED_TAPE_BLOCK
        val accumulatedPlannedTapeBlock = ExternalQualityDetailModel("ACCUMULATED_PLANNED_TAPE_BLOCK", ExternalReportDetailType.ACCUMULATED_PLANNED_TAPE_BLOCK)
        val accumulatedPlannedTapeBlockCalendar = calculateAccumulation(plannedTapeBlock.quantityByCalendars,goodQualityTapeInventoryBlock)
        accumulatedPlannedTapeBlock.quantityByCalendars = accumulatedPlannedTapeBlockCalendar
        detailData.add(accumulatedPlannedTapeBlock)

        //TAPE_REQUIRED_FOR_PRODUCTION_BLOCK
        val tapeRequiredForProductionBlock = ExternalQualityDetailModel("TAPE_REQUIRED_FOR_PRODUCTION_BLOCK", ExternalReportDetailType.TAPE_REQUIRED_FOR_PRODUCTION_BLOCK)
        val tapeRequiredForProductionBlockCalendar : MutableList<KeyValueResponse> = mutableListOf()
        difference2.quantityByCalendars.forEach { x->
            if(x.value?.toInt()!! >0){
                tapeRequiredForProductionBlockCalendar.add(KeyValueResponse(x.key,"0"))
            }else{
                val inverseNumber = x.value?.toInt()!! * -1
                tapeRequiredForProductionBlockCalendar.add(KeyValueResponse(x.key,inverseNumber.toString()))
            }
        }
        tapeRequiredForProductionBlock.quantityByCalendars = tapeRequiredForProductionBlockCalendar
        detailData.add(tapeRequiredForProductionBlock)

        //TAPE_DIFFERENCE_BLOCK
        val tapeDifferenceBlock = ExternalQualityDetailModel("TAPE_DIFFERENCE_BLOCK", ExternalReportDetailType.TAPE_DIFFERENCE_BLOCK)
        val tapeDifferenceBlockCalendar: MutableList<KeyValueResponse> = mutableListOf()

        accumulatedPlannedTapeBlockCalendar.forEach { x ->
            val prepareTapeValue = tapeRequiredForProductionBlockCalendar.firstOrNull { tape -> tape.key == x.key }?.value?.toInt() ?: 0

            val newValue = if ((x.value?.toInt() ?: 0) <= 0) {
                x.value
            } else {
                (x.value?.toInt() ?: 0) - (prepareTapeValue)
            }

            tapeDifferenceBlockCalendar.add(KeyValueResponse(x.key, newValue.toString()))
        }

        tapeDifferenceBlock.quantityByCalendars = tapeDifferenceBlockCalendar
        detailData.add(tapeDifferenceBlock)

        }
        //end
        externalQualityReportModel.details = detailData
    }

    fun calculateDifferenceCalendars(orderQuantityCalendars: List<KeyValueResponse>, productionResultCalendars: List<KeyValueResponse>): MutableList<KeyValueResponse> {
        val difference1Calendars = mutableListOf<KeyValueResponse>()

        orderQuantityCalendars.forEach { orderEntry ->
            val productionEntry = productionResultCalendars.find { it.key == orderEntry.key }
            if (productionEntry != null) {
                val difference = (orderEntry.value?.toIntOrNull() ?: 0) - (productionEntry.value?.toIntOrNull() ?: 0)
                difference1Calendars.add(KeyValueResponse(orderEntry.key, difference.toString()))
            } else {
                difference1Calendars.add(orderEntry)
            }
        }

        return difference1Calendars
    }

    fun addExportType(externalQualityReportModel: ExternalQualityReportModel){
        val exportData : MutableList<KeyValueCustom> = mutableListOf()
        addExportEmpty(exportData,6)
        exportData.add(KeyValueCustom("number_order",externalQualityReportModel.exportType1))
        addExportEmpty(exportData,5)
        if(externalQualityReportModel.exportType2 != null){
            exportData.add(KeyValueCustom("number_order",externalQualityReportModel.exportType2))
            addExportEmpty(exportData,5)
        }
        externalQualityReportModel.exportTypes = exportData


    }

    fun addExportEmpty(list: MutableList<KeyValueCustom>, count: Int) {
        for (i in 1..count) {
            if (i == count) {
                list.add(KeyValueCustom("", "", true))
            } else {
                list.add(KeyValueCustom("", ""))
            }
        }
    }

    fun addKeyValueEmpty(list: MutableList<KeyValueResponse>, count: Int) {
        for (i in 1..count) {
            list.add(KeyValueResponse("", ""))
        }
    }

    fun addShippingData(externalQualityReportModel: ExternalQualityReportModel, valueReportDate: String?,inventoryClosingDate: OffsetDateTime?){
            val inventoryClosingDateMonth = inventoryClosingDate?.month?.value
            val inventoryClosingDateDay   = inventoryClosingDate?.dayOfMonth
            val inventoryTitle =CommonUtils.getMessage("report.export.inventoryTitle",arrayOf(inventoryClosingDateMonth.toString(), inventoryClosingDateDay.toString()))
            val expiredTitle =CommonUtils.getMessage("report.export.expiredTitle",arrayOf((inventoryClosingDateMonth?.plus(1)).toString()))

        val shippingData : MutableList<KeyValueResponse> = mutableListOf()
            shippingData.add(KeyValueResponse("production_plan_title", ExternalReportShippingType.PRODUCTION_PLAN_TITLE))

            val valueNumberOrder = externalQualityReportModel.details.find { it.title.equals(ExternalReportDetailType.ACCUMULATED_ORDER_QUANTITY)  }?.quantityByCalendars?.find { x-> x.key.equals(valueReportDate) }?.value
            shippingData.add(KeyValueResponse("number_order",valueNumberOrder))
            val valueNumberWorkResult = externalQualityReportModel.details.find { it.title.equals(ExternalReportDetailType.ACCUMULATED_PRODUCTION_RESULT)  }?.quantityByCalendars?.find { x-> x.key.equals(valueReportDate) }?.value
            shippingData.add(KeyValueResponse("number_work_result",valueNumberWorkResult))
            shippingData.add(KeyValueResponse("quantity_remaining_title",ExternalReportShippingType.QUANTITY_REMAINING_TITLE))

            val exchangeRateDifferences = externalQualityReportModel.details.find { it.title.equals(ExternalReportDetailType.DIFFERENCE_1)  }?.quantityByCalendars?.find { x-> x.key.equals(valueReportDate) }?.value
            shippingData.add(KeyValueResponse("exchange_rate_differences", exchangeRateDifferences))
            addKeyValueEmpty(shippingData,1)
            shippingData.add(KeyValueResponse("tape_inventory_title",inventoryTitle))
            shippingData.add(KeyValueResponse("tape_inventory_title_number",externalQualityReportModel.tapeInventoryQuantity1.toString()))

            shippingData.add(KeyValueResponse("expired_tape",expiredTitle))
            shippingData.add(KeyValueResponse("expired_tape_number",externalQualityReportModel.tapeExpireQuantity1.toString()))

        addKeyValueEmpty(shippingData,2)

            if(externalQualityReportModel.exportType?.contains(",") == true){
            shippingData.add(KeyValueResponse("tape_inventory_title",inventoryTitle))
            shippingData.add(KeyValueResponse("tape_inventory_title_number",externalQualityReportModel.tapeInventoryQuantity2.toString()))
            shippingData.add(KeyValueResponse("expired_tape",expiredTitle))
            shippingData.add(KeyValueResponse("expired_tape_number",externalQualityReportModel.tapeExpireQuantity2.toString()))
            addKeyValueEmpty(shippingData,2)
            }

            externalQualityReportModel.shippingData = shippingData
    }

    private fun calculateAccumulation(data: List<KeyValueResponse>, firstValue: Int? = null): List<KeyValueResponse> {
        var value = firstValue ?: 0
        val response = mutableListOf<KeyValueResponse>()
        for (item in data) {
            value += (item.value?.toInt() ?: 0)
            response.add(KeyValueResponse(item.key, value.toString()))
        }
        return response
    }

    private fun calculateAccumulationWithInventory(data: List<KeyValueResponse>, inventory: Int? = 0): List<KeyValueResponse> {
        var value =0
        val convertInventory = inventory ?: 0
        val response = mutableListOf<KeyValueResponse>()
        for ((index, item) in data.withIndex()) {
            if(index == 0){ value = convertInventory - (item.value?.toInt() ?: 0)

            }
            else{
                value -= (item.value?.toInt() ?: 0)
            }
            response.add(KeyValueResponse(item.key, value.toString()))
        }
        return response
    }
    //endregion

}