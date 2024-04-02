package com.kcvn.spm.app.report.externalquality.service

import com.kcvn.spm.app.inventoryproduct.payload.response.InventoryProductResponse
import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.service.OrderService
import com.kcvn.spm.app.report.externalquality.payload.model.ExternalQualityDetailModel
import com.kcvn.spm.app.report.externalquality.payload.model.ExternalQualityReportModel
import com.kcvn.spm.app.report.externalquality.payload.request.ExternalQualityReportSearchRequest
import com.kcvn.spm.app.report.externalquality.payload.response.ExternalQualityReportResponse
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.ExternalReportDetailType
import com.kcvn.spm.common.constants.ExternalReportShippingType
import com.kcvn.spm.common.constants.OrderVersion
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.payload.CalendarResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.model.tables.pojos.UpdateTape
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.repository.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ExternalQualityReportService(
    private val orderSer: OrderService,
    private val orderDetailRep: OrderDetailRepository,
    private val workResultRep: WorkResultRepository,
    private val inventoryProductRep: InventoryProductRepository,
    private val productRep: ProductRepository,
    private val completionRateProductRep: CompletionRateProductRepository,
    private val holidaysCalenderRep: HolidaysCalenderRepository,
    private val updateTapeRepository: UpdateTapeRepository
) {

    fun getDataReport(request: ExternalQualityReportSearchRequest, pageable: Pageable): ExternalQualityReportResponse {

        val response = ExternalQualityReportResponse()

        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        response.columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(request.startDate)!!, DateTimeHelper.toTimeZone7(request.endDate)!!, holidayCalenders)
        val daysToSubtract: Long = 10
        response.subColumns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(request.startDate)!!, DateTimeHelper.toTimeZone7(request.endDate)!!, holidayCalenders, daysToSubtract)

        val orderSearchRequest = OrderSearchRequest()
        orderSearchRequest.endDate = request.endDate
        orderSearchRequest.startDate = request.startDate
        orderSearchRequest.productName = request.productName
        orderSearchRequest.version = OrderVersion.LATEST
        val order = orderSer.getPaginatedOrder(orderSearchRequest,pageable)
        val listProductOrder = order.data
        var mappingPaging: List<ExternalQualityReportModel> = listProductOrder!!.map { product ->
            val externalQuality = ExternalQualityReportModel()
            externalQuality.productName = product.productName
            externalQuality.productShortcutName = product.productShortcutName
            externalQuality.blockSh = product.shBlock
            externalQuality
        }
        val listProductName = listProductOrder.map { it.productName }
        val listProduct = productRep.getByName(listProductName.filterNotNull())
        val listCompletionRate = completionRateProductRep.getByProduct(listProductName.filterNotNull())
        val listWorkResult = request.startDate?.let { request.endDate?.let { it1 -> workResultRep.getForReport(it, it1,listProductName) } }
        for(externalQuality in mappingPaging){
            if (listCompletionRate != null) {
                externalQuality.completionRate = listCompletionRate.find { x-> x.productName.equals(externalQuality.productName) }?.rate
            }
        }
        mappingPaging = processMappingPaging(mappingPaging, listProduct)



        //get list name UpdateTape
        val genericNames = getGenericNameUpdateTape(mappingPaging)
        //get list Update Tape
        val updateTapes = updateTapeRepository.getUpdateTapeForReport(genericNames,request.startDate,request.endDate)
        // get list name product
        val productNames = getListNameProduct(mappingPaging)
        // get list Inventory
        val inventoryDetails = inventoryProductRep.getInventoryProductByProductName(productNames,request.endDate)
        //add Details Data here
        for(mappingItem in mappingPaging){
            addDetailsExternalQualityReport(mappingItem,listProductOrder,response.columns,listWorkResult,updateTapes,inventoryDetails)
        }
        val valueReportDate= findValueDateReport(response.columns,response.subColumns)
        //add Shipping Data here
        for(mappingItem in mappingPaging){
            addShippingData(mappingItem,valueReportDate)
        }
        response.data = mappingPaging
        response.totalRecords = order.totalRecords
        return response
    }

    fun getInventoryProduct(productName: String, inventoryProducts:  List<InventoryProductResponse>): Int{
        var data = 0
        data = inventoryProducts.filter { x-> x.productName == productName }.sumOf { x -> x.sheetQuantity!! }
        return data
    }


    fun processMappingPaging(mappingPaging: List<ExternalQualityReportModel>, listProduct: List<Product>): List<ExternalQualityReportModel> {
        val mulMappingPaging = mappingPaging.toMutableList()
        val newExternalQualityList = mutableListOf<ExternalQualityReportModel>()

        for (externalQuality in mulMappingPaging) {
            val product = listProduct.find { it.name == externalQuality.productName }

            if (product != null) {
                if (product.exportType?.contains(",") == true) {
                    val parts = product.exportType?.split(",")
                    val part1 = parts?.get(0)?.trim()
                    val part2 = parts?.getOrNull(1)?.trim()

                    val externalQualityPart1 = externalQuality.copy(
                        mold = product.mold,
                        snapMold = product.snapMold,
                        pcsSh = product.pcsSh,
                        tapeCommon = product.tapeCommon,
                        exportType = part1,
                        blockSh = product.shBlock,
                        layerCount = product.layerCount
                    )
                    newExternalQualityList.add(externalQualityPart1)

                    if (part2 != null) {
                        val externalQualityPart2 = externalQualityPart1.copy(
                            exportType = part2
                        )
                        newExternalQualityList.add(externalQualityPart2)
                    }
                } else {
                    externalQuality.apply {
                        mold = product.mold
                        snapMold = product.snapMold
                        pcsSh = product.pcsSh
                        tapeCommon = product.tapeCommon
                        exportType = product.exportType
                        blockSh = product.shBlock
                        layerCount = product.layerCount

                    }
                    newExternalQualityList.add(externalQuality)
                }
            }
        }

        return newExternalQualityList.distinctBy { it.productName to it.exportType }
    }


    fun getGenericNameUpdateTape(listExternalQuantityReport:  List<ExternalQualityReportModel>) :List<String>{

        val genericNames: MutableList<String> = mutableListOf()

        for(item in listExternalQuantityReport){
            genericNames.add(item.productShortcutName+item.exportType+item.tapeCommon)
        }
        return genericNames
    }

    fun getListNameProduct(listExternalQuantityReport:  List<ExternalQualityReportModel>) :List<String>{

        val productNames: MutableList<String> = mutableListOf()

        for(item in listExternalQuantityReport){
            item.productName?.let { productNames.add(it) }
        }
        return productNames
    }

    fun addDetailsExternalQualityReport(externalQualityReportModel: ExternalQualityReportModel,
                                        listProductOrder:  List<OrderDetailModel>,
                                        columns:  List<CalendarResponse>,
                                        listWorkResult: List<WorkResult>?,
                                        updateTapes:List<UpdateTape>,
                                        inventoryProducts:  List<InventoryProductResponse>){

        //common data
        val detailData : MutableList<ExternalQualityDetailModel> = mutableListOf()
        val genericName=  externalQualityReportModel.productShortcutName+externalQualityReportModel.exportType+externalQualityReportModel.tapeCommon
        val deliveryTypeShortName = externalQualityReportModel.exportType + externalQualityReportModel.productShortcutName
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

        externalQualityReportModel.tapeInventoryQuantity = inventoryTape
        externalQualityReportModel.tapeExpireQuantity = tapeExpired
        externalQualityReportModel.goodQualityTapeInventorySet = goodQualityTapeInventorySet
        externalQualityReportModel.goodQualityTapeInventoryBlock = goodQualityTapeInventoryBlock
        externalQualityReportModel.sumWorkResultQuantity =
            externalQualityReportModel.productName?.let { getInventoryProduct(it,inventoryProducts) }

        //ORDER QUANTITY
        val detailOrderQuantity =  ExternalQualityDetailModel("ORDER_QUANTITY", ExternalReportDetailType.ORDER_QUANTITY)
        val orderQuantityCalendarsSave =listProductOrder.first { x -> x.productName == externalQualityReportModel.productName }.quantityByCalendars!!.toMutableList()
        val orderQuantityCalendars: MutableList<KeyValueResponse> = mutableListOf()
        columns.forEach { (key) ->
            val existingEntry = orderQuantityCalendarsSave.find { it.key == key }
            if (existingEntry == null) { orderQuantityCalendars.add(KeyValueResponse(key, "0")) }
            else{
                orderQuantityCalendars.add(KeyValueResponse(key, value=existingEntry.value))
            }}

        detailOrderQuantity.quantityByCalendars= orderQuantityCalendars
        detailData.add(detailOrderQuantity)

        //ACCUMULATED_ORDER_QUANTITY
        val accumulatedOrderQuantity = ExternalQualityDetailModel("ACCUMULATED_ORDER_QUANTITY", ExternalReportDetailType.ACCUMULATED_ORDER_QUANTITY,externalQualityReportModel.goodQualityTapeInventoryBlock)
        val accumulatedOrderQuantityCalendars = calculateAccumulation(orderQuantityCalendars)
        accumulatedOrderQuantity.quantityByCalendars = accumulatedOrderQuantityCalendars
        detailData.add(accumulatedOrderQuantity)

        //PRODUCTION_RESULT
        val productionResult = ExternalQualityDetailModel("PRODUCTION_RESULT", ExternalReportDetailType.PRODUCTION_RESULT)
        val productionResultCalendars: MutableList<KeyValueResponse> = mutableListOf()
        columns.forEach{ x ->
        val productWorkResult = listWorkResult?.filter { workResult -> x.key.equals(workResult.summaryResultDate?.let { DateTimeHelper.toString(it, DateTimeFormat.yyyyMMdd) })
                                                        && workResult.itemName == externalQualityReportModel.productName }
        val sumItemQuantity = productWorkResult?.map { it.totalTapeQuantity }?.sumOf { it ?: 0 } ?: 0
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
        difference1.quantityByCalendars = calculateDifferenceCalendars(orderQuantityCalendars, productionResultCalendars)
        detailData.add(difference1)

        //DIFFERENCE_2
        val difference2 = ExternalQualityDetailModel("DIFFERENCE_2", ExternalReportDetailType.DIFFERENCE_2)
        difference2.quantityByCalendars = calculateDifferenceCalendars(accumulatedOrderQuantityCalendars,accumulatedProductionResultCalendars)
        detailData.add(difference2)

        //PLANNED_TAPE_SET

        val plannedTapeSet = ExternalQualityDetailModel("PLANNED_TAPE_SET", ExternalReportDetailType.PLANNED_TAPE_SET, externalQualityReportModel.goodQualityTapeInventorySet)
        val plannedTapeSetCalendars: MutableList<KeyValueResponse> = mutableListOf()
        columns.forEach{ x ->
            val tape = updateTape.firstOrNull{ tape -> tape.responseDate?.let { DateTimeHelper.toString(it, DateTimeFormat.yyyyMMdd) } == x.key }
            if(tape!=null){
                plannedTapeSetCalendars.add(KeyValueResponse(x.key, tape.deliveryQuantity.toString()))

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


    fun addShippingData(externalQualityReportModel: ExternalQualityReportModel, valueReportDate: String?){
            val shippingData : MutableList<KeyValueResponse> = mutableListOf()
            shippingData.add(KeyValueResponse("production_plan_title", ExternalReportShippingType.PRODUCTION_PLAN_TITLE))

            val valueNumberOrder = externalQualityReportModel.details.find { it.title.equals(ExternalReportDetailType.ACCUMULATED_ORDER_QUANTITY)  }?.quantityByCalendars?.find { x-> x.key.equals(valueReportDate) }?.value
            shippingData.add(KeyValueResponse("number_order",valueNumberOrder))

            val valueNumberWorkResult = externalQualityReportModel.details.find { it.title.equals(ExternalReportDetailType.ACCUMULATED_PRODUCTION_RESULT)  }?.quantityByCalendars?.find { x-> x.key.equals(valueReportDate) }?.value
            shippingData.add(KeyValueResponse("number_work_result",valueNumberWorkResult))
            shippingData.add(KeyValueResponse("",""))
            shippingData.add(KeyValueResponse("quantity_remaining_title",ExternalReportShippingType.QUANTITY_REMAINING_TITLE))

            val numberOrder = shippingData.find { it.key == "number_order" }?.value?.toIntOrNull() ?: 0
            val numberWorkResult = shippingData.find { it.key == "number_work_result" }?.value?.toIntOrNull() ?: 0
            val exchangeRateDifferences = (numberOrder - numberWorkResult).toString()
            shippingData.add(KeyValueResponse("exchange_rate_differences", exchangeRateDifferences))
            shippingData.add(KeyValueResponse("tape_inventory_title",ExternalReportShippingType.TAPE_INVENTORY_TITLE))
            shippingData.add(KeyValueResponse("tape_inventory_title_number",externalQualityReportModel.tapeInventoryQuantity.toString()))
            shippingData.add(KeyValueResponse("expired_tape",ExternalReportShippingType.EXPIRED_TAPE))
            shippingData.add(KeyValueResponse("expired_tape_number",externalQualityReportModel.tapeExpireQuantity.toString()))
            shippingData.add(KeyValueResponse("",""))
            shippingData.add(KeyValueResponse("",""))
            externalQualityReportModel.shippingData = shippingData
    }

    fun findValueDateReport(reportList:List<CalendarResponse>, accumulatedReportList:List<CalendarResponse>): String? {

        val valueMap = reportList.firstOrNull()?.value
        val result=  accumulatedReportList.find { x-> x.value == valueMap }?.key

        return result
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


}