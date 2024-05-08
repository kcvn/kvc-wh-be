package com.kcvn.spm.app.plan.controller

import com.kcvn.spm.app.plan.payload.model.ProductPlanModel
import com.kcvn.spm.app.plan.payload.request.*
import com.kcvn.spm.app.plan.payload.response.ProductPlanDetailResponse
import com.kcvn.spm.app.plan.service.PlanHistoryService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/plan/history")
class PlanHistoryController (private val planHistoryService: PlanHistoryService){
    @GetMapping("")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_WORK_PLAN.value) || hasRole('ADMIN')")
    fun create(request: PlanHistoryRequest): ResponseEntity<BaseResponse<List<FileContentModel>>> {
        val data = planHistoryService.planHistory(request)
        return ResponseEntity<BaseResponse<List<FileContentModel>>>(data, HttpStatus.OK)
    }

    @DeleteMapping("/{fileName}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_WORK_PLAN.value) || hasRole('ADMIN')")
    fun removeFile(@PathVariable fileName: String): ResponseEntity<Unit> {
        planHistoryService.removeFile(fileName)
        return ResponseEntity(Unit, HttpStatus.OK)
    }

    @GetMapping("/download")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_WORK_PLAN.value) || hasRole('ADMIN')")
    fun downloadFile(@RequestParam fileName: String): ResponseEntity<BaseResponse<FileContentModel>>{
        val data = planHistoryService.downloadFile(fileName)
        return ResponseEntity(data, HttpStatus.OK)
    }


    @GetMapping("/getPlansByMonth")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_WORK_PLAN.value) || hasRole('ADMIN')")
    fun getPlansByMonth(@RequestParam month: String): ResponseEntity<BaseResponse<List<DropdownResponse>>>{
        val data = planHistoryService.getPlansByMonth(month)
        return ResponseEntity(data, HttpStatus.OK)
    }
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_WORK_PLAN.value) || hasRole('ADMIN')")
    fun getList(
        request: PlanHistorySearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<ProductPlanModel>> {
        val data = planHistoryService.getListPlanByVersion(request, pageable)
        return ResponseEntity<BasePagingResponse<ProductPlanModel>>(data, HttpStatus.OK)
    }


//    @GetMapping("/detail")
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_WORK_PLAN.value) || hasRole('ADMIN')")
//    fun getPlanDetail(request: PlanHistoryDetailRequest): ResponseEntity<ProductPlanDetailResponse> {
//        val data = planHistoryService.getPlanDetail(request)
//        return ResponseEntity<ProductPlanDetailResponse>(data, HttpStatus.OK)
//    }
}