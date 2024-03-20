package com.kcvn.spm.app.report.externalquality.service

import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.report.externalquality.payload.model.ExternalQualityReportModel
import com.kcvn.spm.app.report.externalquality.payload.request.ExternalQualityReportSearchRequest
import com.kcvn.spm.app.report.externalquality.payload.response.ExternalQualityReportResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.repository.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class ExternalQualityReportService(
    private val orderRep: OrderRepository,
    private val orderDetailRep: OrderDetailRepository,
    private val workResultRep: WorkResultRepository,
    private val inventoryProductRep: InventoryProductRepository,
    private val productRep: ProductRepository,
    private val completionRateProductRep: CompletionRateProductRepository
) {

    fun getDataReport(request: ExternalQualityReportSearchRequest, pageable: Pageable): ExternalQualityReportResponse {

        val response = ExternalQualityReportResponse()

        val orderSearchRequest = OrderSearchRequest()
        orderSearchRequest.endDate = request.endDate
        orderSearchRequest.startDate = request.startDate
        orderSearchRequest.productName = request.productName
        val order = orderRep.getPagingListOrder(orderSearchRequest,pageable)
        val listProductOrder = order.first
        val mappingPaging: List<ExternalQualityReportModel> = listProductOrder.map { product ->
            val externalQuality = ExternalQualityReportModel()
            externalQuality.productName = product.productName
            externalQuality.pcsSh = product.pcsSh
            externalQuality.productShortcutName = product.productShortcutName
            externalQuality
        }
        val listProductName = listProductOrder.map { it.productName }

        val listProduct = productRep.getByName(listProductName.filterNotNull())
        val listCompletionRate = completionRateProductRep.getByProduct(listProductName.filterNotNull())

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

        response.data = mappingPaging

        return response
    }

}