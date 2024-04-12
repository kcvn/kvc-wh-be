package com.kcvn.spm.app.plan.controller

import com.kcvn.spm.app.plan.payload.request.CreatePlanRequest
import com.kcvn.spm.app.plan.service.CreatePlanService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/plan/create")
class CreatePlanController (private val createPlanService: CreatePlanService) {

    @GetMapping("/check-inventory")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_ORDER.value) || hasRole('ADMIN')")
    fun checkInventory(request: CreatePlanRequest): ResponseEntity<BaseResponse<Boolean>> {
        val data = createPlanService.checkInventory(request)
        return ResponseEntity<BaseResponse<Boolean>>(data, HttpStatus.OK)
    }

    @PostMapping("")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_ORDER.value) || hasRole('ADMIN')")
    fun create(@RequestBody request: CreatePlanRequest): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = createPlanService.createPlan(request)
        return ResponseEntity<BaseResponse<FileContentModel>>(data, HttpStatus.OK)
    }
}