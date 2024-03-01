package com.kcvn.spm.app.report.quantityreport.controller

import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.app.report.quantityreport.service.QuantityReportService
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.model.tables.pojos.CalculateQuantityResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
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
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CA_REPORT_QUANTITY.value) || hasRole('ADMIN')")
    fun CalculateQuantity(request: CalculateQuantityRequest): ResponseEntity<BaseResponse<Boolean>> {
        val data = quantityReportService.calculateQuantity(request)
        return ResponseEntity<BaseResponse<Boolean>>(data, HttpStatus.OK)
    }

    @GetMapping("/locked-quantity")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).LOCK_REPORT_QUANTITY.value) || hasRole('ADMIN')")
    fun LockedQuantity(request: String): ResponseEntity<BaseResponse<Boolean>> {
        val result = quantityReportService.lockedQuantity(request)
        return ResponseEntity<BaseResponse<Boolean>>(result, HttpStatus.OK)
    }

    @GetMapping("/get-list-calculate-quantity-result")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_REPORT_QUANTITY.value) || hasRole('ADMIN')")
    fun getListCalculateQuantityResult(
        @PageableDefault(size = 10, page = 0)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["monthReport"], direction = Sort.Direction.DESC)
        ) pageable: Pageable,
    ): ResponseEntity<BasePagingResponse<CalculateQuantityResult>> {
        val result = quantityReportService.getListCalculateQuantityResult(pageable)
        return ResponseEntity<BasePagingResponse<CalculateQuantityResult>>(result, HttpStatus.OK)
    }

}