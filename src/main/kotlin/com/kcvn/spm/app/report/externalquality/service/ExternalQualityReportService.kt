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
        val order = orderRep.getPagingListOrder(orderSearchRequest,pageable)
        val listProduct = order.first
        val mappingPaging: List<ExternalQualityReportModel> = listProduct.map { product ->
            val externalQuality = ExternalQualityReportModel()
            externalQuality.productName = product.productName
            externalQuality.blockSh = product.shBlock
            externalQuality.pcsSh = product.pcsSh
            externalQuality.productShortcutName = product.productShortcutName
            externalQuality
        }
        val listProductName = listProduct.map { it.productName }
        val listCompletionRate = completionRateProductRep.getByProduct(listProductName.filterNotNull())

        response.data = mappingPaging



        return response
    }

}