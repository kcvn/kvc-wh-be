package com.kcvn.spm.app.report.externalquality.service

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
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.ExcelHelper
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.CalendarResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.UpdateTape
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.repository.*
import org.apache.poi.xssf.usermodel.XSSFWorkbook
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream


@Service
@Transactional
class ExternalQualityReportService(
    private val orderSer: OrderService,
    private val workResultRep: WorkResultRepository,
    private val inventoryProductRep: InventoryProductRepository,
    private val completionRateProductRep: CompletionRateProductRepository,
    private val holidaysCalenderRep: HolidaysCalenderRepository,
    private val updateTapeRepository: UpdateTapeRepository,
    private val orderInfoRepository: OrderInfoRepository,
) {

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
                        setCellValueCustom(workbook,dataRow, 9, style, productReport.completionRate.toString(), isAlignCenter = true)
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



    fun getDataReport(request: ExternalQualityReportSearchRequest, pageable: Pageable): ExternalQualityReportResponse {
        val startDate = DateTimeHelper.toTimeZone7(request.startDate)
        val endDate = DateTimeHelper.toTimeZone7(request.endDate)
        val response = ExternalQualityReportResponse()
        val orderInfo = orderInfoRepository.getListOrderForReport(request,pageable)
        val mappingPaging = orderInfo.first
        val listProductName = mappingPaging.map { it.productName }
        response.totalRecords = orderInfo.second

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        response.columns = DateTimeHelper.toCalendarColumn(startDate!!, endDate!!, holidayCalenders)
        val daysToSubtract: Long = 10

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

        val listCompletionRate = completionRateProductRep.getByProduct(listProductName.filterNotNull())
        val listWorkResult = request.startDate?.let { request.endDate?.let { it1 -> workResultRep.getForReport(it, it1,listProductName) } }
        for(externalQuality in mappingPaging){
            if (listCompletionRate != null) {
                externalQuality.completionRate = listCompletionRate.find { x-> x.productName.equals(externalQuality.productName) }?.rate
            }
        }
        //get list name UpdateTape
        val genericNames = getGenericNameUpdateTape(mappingPaging)
        //get list Update Tape
        val updateTapes = updateTapeRepository.getUpdateTapeForReport(genericNames,startDate,endDate)
        // get list name product
        val productNames = getListNameProduct(mappingPaging)
        // get list Inventory
        val inventoryDetails = inventoryProductRep.getInventoryProductByProductName(productNames,endDate)
        //create list data exist
        val listDataExist :MutableList<ExternalQualityDetailExistModel> = mutableListOf()
        //add Details Data here
        for(mappingItem in mappingPaging){
            addDetailsExternalQualityReport(mappingItem,listProductOrder,response.columns,listWorkResult,updateTapes,inventoryDetails, subColumns,listDataExist)
        }
        val valueReportDate= request.endDate?.let { DateTimeHelper.toString(it, DateTimeFormat.yyyyMMdd) }
        //add Shipping Data here
        for(mappingItem in mappingPaging){
            addShippingData(mappingItem,valueReportDate)
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

    fun getGenericNameUpdateTape(listExternalQuantityReport:  List<ExternalQualityReportModel>) :List<String>{

        val genericNames: MutableList<String> = mutableListOf()

        for(item in listExternalQuantityReport){
            genericNames.add(item.productShortcutName+item.exportType+item.tapeCommon)
        }
        return genericNames
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

    fun getListNameProduct(listExternalQuantityReport:  List<ExternalQualityReportModel>) :List<String>{

        val productNames: MutableList<String> = mutableListOf()

        for(item in listExternalQuantityReport){
            item.productName?.let { productNames.add(it) }
        }
        return productNames
    }

    fun addDetailsExternalQualityReport(externalQualityReportModel: ExternalQualityReportModel,
                                        listProductOrder:  List<OrderDetailModel>?,
                                        columns:  List<CalendarResponse>,
                                        listWorkResult: List<WorkResult>?,
                                        updateTapes:List<UpdateTape>,
                                        inventoryProducts:  List<InventoryProductResponse>,
                                        subColumns: List<CalendarResponse>,
                                        listDataExist: MutableList<ExternalQualityDetailExistModel> = mutableListOf()){

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
        val accumulatedOrderQuantity = ExternalQualityDetailModel("ACCUMULATED_ORDER_QUANTITY", ExternalReportDetailType.ACCUMULATED_ORDER_QUANTITY,externalQualityReportModel.sumWorkResultQuantity)
        val accumulatedOrderQuantityCalendars = calculateAccumulation(orderQuantityCalendars)
        accumulatedOrderQuantity.quantityByCalendars = accumulatedOrderQuantityCalendars
        detailData.add(accumulatedOrderQuantity)

        //PRODUCTION_RESULT
        val productionResult = ExternalQualityDetailModel("PRODUCTION_RESULT", ExternalReportDetailType.PRODUCTION_RESULT)
        val productionResultCalendars: MutableList<KeyValueResponse> = mutableListOf()
        columns.forEach{ x ->
        val productWorkResult = listWorkResult?.filter { workResult -> x.key.equals(workResult.summaryResultDate?.let { DateTimeHelper.toString(it, DateTimeFormat.yyyyMMdd) })
                                                        && workResult.itemName == externalQualityReportModel.productName }
        val sumItemQuantity = productWorkResult?.map { it.unfinishedQuantity }?.sumOf { it ?: 0 } ?: 0
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
        difference1.quantityByCalendars = calculateDifferenceCalendars(productionResultCalendars,orderQuantityCalendars)
        detailData.add(difference1)

        //DIFFERENCE_2
        val difference2 = ExternalQualityDetailModel("DIFFERENCE_2", ExternalReportDetailType.DIFFERENCE_2)
        difference2.quantityByCalendars = calculateAccumulationWithInventory(orderQuantityCalendars,externalQualityReportModel.sumWorkResultQuantity)
        detailData.add(difference2)

        // LOGIC TAPE

    val exportTypes = parseExportTypes(externalQualityReportModel.exportType)

    var isFirstExportType = true
    for(exportType in exportTypes){
        val genericName=  externalQualityReportModel.productShortcutName+exportType+externalQualityReportModel.tapeCommon
        val deliveryTypeShortName = exportType + externalQualityReportModel.productShortcutName
        val sharedTape = externalQualityReportModel.tapeCommon
        val updateTape = updateTapes.filter { x-> x.genericName == genericName }
        val updateTapeCommon = updateTape.filter { x-> x.deliveryTypeShortName == deliveryTypeShortName && x.sharedTape ==  sharedTape}
        var inventoryTape = 0
        var tapeExpired =0
        if(updateTapeCommon.isNotEmpty()){
            tapeExpired = updateTapeCommon.filter { it.totalNg != null }.sumOf { it.totalNg!! }
            inventoryTape = updateTapeCommon.filter { it.logPd1Inventory != null }.sumOf { it.logPd1Inventory!! }
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
        externalQualityReportModel.sumWorkResultQuantity =
            externalQualityReportModel.productName?.let { getInventoryProduct(it,inventoryProducts) }

        //PLANNED_TAPE_SET
        val plannedTapeSet = ExternalQualityDetailModel("PLANNED_TAPE_SET", ExternalReportDetailType.PLANNED_TAPE_SET, externalQualityReportModel.goodQualityTapeInventorySet)
        val plannedTapeSetCalendars: MutableList<KeyValueResponse> = mutableListOf()
        subColumns.forEach{ x ->
            val tape = updateTape.filter{ tape -> tape.responseDate?.let { DateTimeHelper.toTimeZone7((it.plusDays(10)))
                ?.let { it1 -> DateTimeHelper.toString(it1, DateTimeFormat.yyyyMMdd) } } == x.key }
            if(tape.isNotEmpty()){
                plannedTapeSetCalendars.add(KeyValueResponse(x.key, tape.sumOf { it.deliveryQuantity ?: 0 }.toString()))
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
    fun addShippingData(externalQualityReportModel: ExternalQualityReportModel, valueReportDate: String?){
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
            shippingData.add(KeyValueResponse("tape_inventory_title",ExternalReportShippingType.TAPE_INVENTORY_TITLE))
            shippingData.add(KeyValueResponse("tape_inventory_title_number",externalQualityReportModel.tapeInventoryQuantity1.toString()))
            shippingData.add(KeyValueResponse("expired_tape",ExternalReportShippingType.EXPIRED_TAPE))
            shippingData.add(KeyValueResponse("expired_tape_number",externalQualityReportModel.tapeExpireQuantity1.toString()))
            addKeyValueEmpty(shippingData,2)

            if(externalQualityReportModel.exportType?.contains(",") == true){
                shippingData.add(KeyValueResponse("tape_inventory_title",ExternalReportShippingType.TAPE_INVENTORY_TITLE))
                shippingData.add(KeyValueResponse("tape_inventory_title_number",externalQualityReportModel.tapeInventoryQuantity2.toString()))
                shippingData.add(KeyValueResponse("expired_tape",ExternalReportShippingType.EXPIRED_TAPE))
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

}