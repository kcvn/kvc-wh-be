package com.kcvn.spm.app.report.externalquality.service

import com.kcvn.spm.app.report.externalquality.payload.request.ExternalQualityReportSearchRequest
import com.kcvn.spm.app.report.externalquality.payload.response.ExternalQualityReportResponse
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

        return ExternalQualityReportResponse()
    }

}