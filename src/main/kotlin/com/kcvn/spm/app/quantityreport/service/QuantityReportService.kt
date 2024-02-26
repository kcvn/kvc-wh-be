package com.kcvn.spm.app.quantityreport.service

import com.kcvn.spm.app.completionrate.service.CompletionRateService
import com.kcvn.spm.app.order.service.OrderService
import com.kcvn.spm.app.product.service.ProductService
import com.kcvn.spm.app.productprocess.service.ProductProcessService
import com.kcvn.spm.app.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.app.quantityreport.payload.response.CalculateQuantityResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class QuantityReportService(
    private val orderService: OrderService,
    private val productService: ProductService,
    private val productProcessService : ProductProcessService,
    private val completionRateService : CompletionRateService
) {
    fun calculateQuantity(request: CalculateQuantityRequest): BasePagingResponse<CalculateQuantityResponse> {
        TODO("Not yet implemented")
    }

    fun lockedQuantity(request: String): BaseResponse<Boolean> {
        try {

        }catch(e: IllegalStateException){

        }
        return BaseResponse(true)
    }

}