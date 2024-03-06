package com.kcvn.spm.app.plan.controller

import com.kcvn.spm.app.plan.payload.model.ProductPlanModel
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.ProductPlanDetailResponse
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/plan")
class PlanController {
    @GetMapping("/get-list")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_ORDER.value) || hasRole('ADMIN')")
    fun getList(
        request: PlanSearchRequest?,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<ProductPlanModel>> {
        val data = BasePagingResponse<ProductPlanModel>()
        return ResponseEntity<BasePagingResponse<ProductPlanModel>>(data, HttpStatus.OK)
    }

    @GetMapping("/detail")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_ORDER.value) || hasRole('ADMIN')")
    fun getPlanDetail(
        @RequestParam id: String
    ): ResponseEntity<ProductPlanDetailResponse> {
        val data = ProductPlanDetailResponse()
        return ResponseEntity<ProductPlanDetailResponse>(data, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_ORDER.value) || hasRole('ADMIN')")
    fun exportExcel(
        request: PlanSearchRequest?,
        @PageableDefault(size = PagingDefault.EXPORT_SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = BaseResponse<FileContentModel>()
        return ResponseEntity(data, HttpStatus.OK)
    }
}