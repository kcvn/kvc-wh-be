package com.kcvn.spm.app.plan.controller

import com.kcvn.spm.app.plan.payload.request.PlanHistoryRequest
import com.kcvn.spm.app.plan.service.PlanHistoryService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
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

}