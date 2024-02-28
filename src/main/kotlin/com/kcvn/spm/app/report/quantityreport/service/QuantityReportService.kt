package com.kcvn.spm.app.report.quantityreport.service

import com.kcvn.spm.app.completionrate.service.CompletionRateService
import com.kcvn.spm.app.order.service.OrderService
import com.kcvn.spm.app.product.service.ProductService
import com.kcvn.spm.app.productprocess.service.ProductProcessService
import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.app.report.quantityreport.payload.response.CalculateQuantityResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.repository.CalculateQuantityReportRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime

@Service
@Transactional
class QuantityReportService(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val productProcessService: ProductProcessService,
    private val completionRateService: CompletionRateService,
    private val calculateQuantityReportRep: CalculateQuantityReportRepository,
) {
    fun calculateQuantity(request: CalculateQuantityRequest): BasePagingResponse<CalculateQuantityResponse> {
        TODO("Not yet implemented")
    }

    fun lockedQuantity(request: String): BaseResponse<Boolean> {
        val calculateQuantityReport = calculateQuantityReportRep.findById(request)
        calculateQuantityReportRep.update(calculateQuantityReport)
        return BaseResponse(true, message = CommonUtils.getMessage("locked.success"))
    }

}