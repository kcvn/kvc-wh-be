package com.kcvn.spm.app.report.externalquality.service

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
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.payload.CalendarResponse
import com.kcvn.spm.common.payload.KeyValueResponse
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
    private val holidaysCalenderRep: HolidaysCalenderRepository
) {

    fun getDataReport(request: ExternalQualityReportSearchRequest, pageable: Pageable): ExternalQualityReportResponse {

        val response = ExternalQualityReportResponse()
        val orderSearchRequest = OrderSearchRequest()
        val holidayCalenders = holidaysCalenderRep.getHolidaysCalender()
        response.columns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(request.startDate)!!, DateTimeHelper.toTimeZone7(request.endDate)!!, holidayCalenders)
        val daysToSubtract: Long = 10
        response.subColumns = DateTimeHelper.toCalendarColumn(DateTimeHelper.toTimeZone7(request.startDate)!!, DateTimeHelper.toTimeZone7(request.endDate)!!, holidayCalenders, daysToSubtract)
        orderSearchRequest.endDate = request.endDate
        orderSearchRequest.startDate = request.startDate
        orderSearchRequest.productName = request.productName
        val order = orderSer.getPaginatedOrder(orderSearchRequest,pageable)
        val listProductOrder = order.data
        val mappingPaging: List<ExternalQualityReportModel> = listProductOrder!!.map { product ->
            val externalQuality = ExternalQualityReportModel()
            externalQuality.productName = product.productName
            externalQuality.productShortcutName = product.productShortcutName
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
        for(externalQuality in mappingPaging){
                val product = listProduct.find { x-> x.name.equals(externalQuality.productName) }
                if(product !=null){
                    externalQuality.mold = product.mold
                    externalQuality.snapMold = product.snapMold
                    externalQuality.pcsSh = product.pcsSh
                    externalQuality.tapeCommon = product.tapeCommon
                    externalQuality.exportType = product.exportType
                    externalQuality.blockSh = product.shBlock
                }
        }

        //add Details Data here
        for(mappingItem in mappingPaging){
            addDetailsExternalQualityReport(mappingItem,listProductOrder,response.columns,listWorkResult)

        }

        //add Shipping Data here
        for(mappingItem in mappingPaging){
            addShippingData(mappingItem)
        }




        response.data = mappingPaging
        return response
    }

    fun addDetailsExternalQualityReport(externalQualityReportModel: ExternalQualityReportModel,
                                        listProductOrder:  List<OrderDetailModel>,
                                        columns:  List<CalendarResponse>,
                                        listWorkResult: List<WorkResult>?){

        val detailData : MutableList<ExternalQualityDetailModel> = mutableListOf()

        //order quantity
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
        val accumulatedOrderQuantity = ExternalQualityDetailModel("ACCUMULATED_ORDER_QUANTITY", ExternalReportDetailType.ACCUMULATED_ORDER_QUANTITY)
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
        difference1.quantityByCalendars = difference1Calendars

        detailData.add(difference1)

        //DIFFERENCE_2
        val difference2 = ExternalQualityDetailModel("DIFFERENCE_2", ExternalReportDetailType.DIFFERENCE_2)
        val difference2Calendars = mutableListOf<KeyValueResponse>()
        accumulatedOrderQuantityCalendars.forEach { orderEntry ->
            val productionEntry = accumulatedProductionResultCalendars.find { it.key == orderEntry.key }
            if (productionEntry != null) {
                val difference = (orderEntry.value?.toIntOrNull() ?: 0) - (productionEntry.value?.toIntOrNull() ?: 0)
                difference1Calendars.add(KeyValueResponse(orderEntry.key, difference.toString()))
            } else {
                difference1Calendars.add(orderEntry)
            }
        }
        difference2.quantityByCalendars = difference2Calendars

        detailData.add(difference2)


        val plannedTapeSet = ExternalQualityDetailModel("PLANNED_TAPE_SET", ExternalReportDetailType.PLANNED_TAPE_SET)
        detailData.add(plannedTapeSet)

        val accumulatedPlannedTapeSet = ExternalQualityDetailModel("ACCUMULATED_PLANNED_TAPE_SET", ExternalReportDetailType.ACCUMULATED_PLANNED_TAPE_SET)
        detailData.add(accumulatedPlannedTapeSet)

        val plannedTapeBlock = ExternalQualityDetailModel("PLANNED_TAPE_BLOCK", ExternalReportDetailType.PLANNED_TAPE_BLOCK)
        detailData.add(plannedTapeBlock)

        val accumulatedPlannedTapeBlock = ExternalQualityDetailModel("ACCUMULATED_PLANNED_TAPE_BLOCK", ExternalReportDetailType.ACCUMULATED_PLANNED_TAPE_BLOCK)
        detailData.add(accumulatedPlannedTapeBlock)

        val tapeRequiredForProductionBlock = ExternalQualityDetailModel("TAPE_REQUIRED_FOR_PRODUCTION_BLOCK", ExternalReportDetailType.TAPE_REQUIRED_FOR_PRODUCTION_BLOCK)
        detailData.add(tapeRequiredForProductionBlock)

        val tapeDifferenceBlock = ExternalQualityDetailModel("TAPE_DIFFERENCE_BLOCK", ExternalReportDetailType.TAPE_DIFFERENCE_BLOCK)
        detailData.add(tapeDifferenceBlock)








        //end
        externalQualityReportModel.details = detailData


    }

    fun addShippingData(externalQualityReportModel: ExternalQualityReportModel){

            val shippingData : MutableList<KeyValueResponse> = mutableListOf()
            shippingData.add(KeyValueResponse("production_plan_title", ExternalReportShippingType.PRODUCTION_PLAN_TITLE))
            shippingData.add(KeyValueResponse("number_order","80"))
            shippingData.add(KeyValueResponse("number_work_result","50"))
            shippingData.add(KeyValueResponse("",""))
            shippingData.add(KeyValueResponse("quantity_remaining_title",ExternalReportShippingType.QUANTITY_REMAINING_TITLE))

            val numberOrder = shippingData.find { it.key == "number_order" }?.value?.toIntOrNull() ?: 0
            val numberWorkResult = shippingData.find { it.key == "number_work_result" }?.value?.toIntOrNull() ?: 0
            val exchangeRateDifferences = (numberOrder - numberWorkResult).toString()
            shippingData.add(KeyValueResponse("exchange_rate_differences", exchangeRateDifferences))

            shippingData.add(KeyValueResponse("tape_inventory_title",ExternalReportShippingType.TAPE_INVENTORY_TITLE))
            shippingData.add(KeyValueResponse("tape_inventory_title_number","4000"))
            shippingData.add(KeyValueResponse("expired_tape",ExternalReportShippingType.EXPIRED_TAPE))
            shippingData.add(KeyValueResponse("expired_tape_number","0"))
            shippingData.add(KeyValueResponse("",""))
            shippingData.add(KeyValueResponse("",""))
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


}