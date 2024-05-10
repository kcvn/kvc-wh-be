package com.kcvn.spm.app.plan.controller

import com.kcvn.spm.app.plan.payload.model.ProductPlanModel
import com.kcvn.spm.app.plan.payload.request.PlanDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.PlanSummaryResponse
import com.kcvn.spm.app.plan.payload.response.ProductPlanDetailResponse
import com.kcvn.spm.app.plan.service.PlanService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/plan")
class PlanController(private val planService: PlanService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_INFO_WORK_PLAN.value) || hasRole('ADMIN')")
    fun getList(
        request: PlanSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<ProductPlanModel>> {
        val data = planService.getListPlan(request, pageable)
        return ResponseEntity<BasePagingResponse<ProductPlanModel>>(data, HttpStatus.OK)
    }

    @GetMapping("/detail")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_INFO_WORK_PLAN.value) || hasRole('ADMIN')")
    fun getPlanDetail(request: PlanDetailRequest): ResponseEntity<ProductPlanDetailResponse> {
        val data = planService.getPlanDetail(request)
        return ResponseEntity<ProductPlanDetailResponse>(data, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_INFO_WORK_PLAN.value) || hasRole('ADMIN')")
    fun exportExcel(request: PlanSearchRequest): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = planService.exportExcel(request)
        return ResponseEntity(BaseResponse(data), HttpStatus.OK)
    }

    @GetMapping("/summary")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_INFO_WORK_PLAN.value) || hasRole('ADMIN')")
    fun getListSummary(request: PlanSearchRequest): ResponseEntity<PlanSummaryResponse> {
        val data = planService.getPlanSummary(request)
        return ResponseEntity<PlanSummaryResponse>(data, HttpStatus.OK)
    }

    @PostMapping("/approve")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).AP_INFO_WORK_PLAN.value) || hasRole('ADMIN')")
    fun approve(request: PlanSearchRequest, fileName: String?): ResponseEntity<BaseResponse<Boolean>> {
        val data = planService.approve(request,fileName)
        return ResponseEntity<BaseResponse<Boolean>>(data, HttpStatus.OK)
    }

    @PostMapping("/check-temp")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_INFO_WORK_PLAN.value) || hasRole('ADMIN')")
    fun checkTemp(): ResponseEntity<BaseResponse<Boolean>> {
        val data = planService.checkTemp()
        return ResponseEntity<BaseResponse<Boolean>>(data, HttpStatus.OK)
    }
}