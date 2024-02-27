package com.kcvn.spm.app.quantityreport.controller

import com.kcvn.spm.app.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.app.quantityreport.payload.response.CalculateQuantityResponse
import com.kcvn.spm.app.quantityreport.service.QuantityReportService
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/quantity-report")
class QuantityReportController(
    private val quantityReportService: QuantityReportService,
) {
    @PostMapping("/calculate-quantity")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_WORK_RESULT.value) || hasRole('ADMIN')")
    fun CalculateQuantity(
        request: CalculateQuantityRequest,
        @PageableDefault(size = 10, page = 0)
        @SortDefault.SortDefaults(
        ) pageable: Pageable,
    ): ResponseEntity<BasePagingResponse<CalculateQuantityResponse>> {
        val data = quantityReportService.calculateQuantity(request)
        return ResponseEntity<BasePagingResponse<CalculateQuantityResponse>>(data, HttpStatus.OK)
    }

    @GetMapping("/locked-quantity")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_WORK_RESULT.value) || hasRole('ADMIN')")
    fun LockedQuantity(request: String) : ResponseEntity<BaseResponse<Boolean>> {
        val result = quantityReportService.lockedQuantity(request)
        return ResponseEntity<BaseResponse<Boolean>>(result,HttpStatus.OK)
    }

}