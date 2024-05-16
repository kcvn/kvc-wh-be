package com.kcvn.spm.app.plan.controller

import com.kcvn.spm.app.plan.payload.model.PlanCalendarConfigModel
import com.kcvn.spm.app.plan.payload.request.PlanCalendarConfigGetRequest
import com.kcvn.spm.app.plan.payload.request.PlanCalendarConfigUpdateRequest
import com.kcvn.spm.app.plan.service.PlanCalendarConfigService
import com.kcvn.spm.common.payload.BaseResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/plan/calendar-config")
class PlanCalendarConfigController(private val planCalendarConfigService: PlanCalendarConfigService) {
    @GetMapping("")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_INFO_WORK_PLAN.value) || hasRole('ADMIN')")
    fun getConfig(request: PlanCalendarConfigGetRequest): ResponseEntity<BaseResponse<PlanCalendarConfigModel>> {
        val data = planCalendarConfigService.getConfigByMonth(request)
        return ResponseEntity<BaseResponse<PlanCalendarConfigModel>>(data, HttpStatus.OK)
    }

    @PostMapping("")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_INFO_WORK_PLAN.value) || hasRole('ADMIN')")
    fun create(@RequestBody request: PlanCalendarConfigUpdateRequest): ResponseEntity<BaseResponse<Boolean>> {
        val data = planCalendarConfigService.updateConfig(request)
        return ResponseEntity<BaseResponse<Boolean>>(data, HttpStatus.OK)
    }
}