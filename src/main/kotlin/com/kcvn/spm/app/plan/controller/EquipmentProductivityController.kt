package com.kcvn.spm.app.plan.controller


import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.app.plan.payload.response.PagingEquipmentProdResponse
import com.kcvn.spm.app.plan.service.PlanService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/plan/equipment-productivity/")
class EquipmentProductivityController(
    private val planService: PlanService
) {

    @GetMapping("get-all")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_ORDER.value) || hasRole('ADMIN')")
    fun getList(
        request: PlanSearchRequest
    ): ResponseEntity<PagingEquipmentProdResponse> {

        val data = planService.getEquipmentProductivityPlan(request)
        return ResponseEntity<PagingEquipmentProdResponse>(data, HttpStatus.OK)
    }

    @PostMapping(value = ["import-excel"], consumes = ["multipart/form-data"])
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun importExcel(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = planService.importExcelEquipment(file)
        return ResponseEntity(data, HttpStatus.OK)
    }
}