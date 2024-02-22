package com.kcvn.spm.app.quantityreport.service

import com.kcvn.spm.app.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.app.quantityreport.payload.response.CalculateQuantityResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class QuantityReportService {
    fun calculateQuantity(request: CalculateQuantityRequest): BasePagingResponse<CalculateQuantityResponse> {
        TODO("Not yet implemented")
    }
}