package com.kcvn.spm.app.plan.controller


import com.kcvn.spm.app.plan.payload.request.PlanDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanProcessDetailRequest
import com.kcvn.spm.app.plan.payload.response.PagingEquipmentProdResponse
import com.kcvn.spm.app.plan.service.EquipmentProductivityService
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/plan/equipment-productivity/")
class EquipmentProductivityController(
    private val equipmentProductivityService: EquipmentProductivityService)
{


    @GetMapping("export-excel")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_ORDER.value) || hasRole('ADMIN')")
    fun exportExcel(
    request: PlanSearchRequest,
    @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
    pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = equipmentProductivityService.exportExcel(request,pageable)
        return ResponseEntity(BaseResponse(data), HttpStatus.OK)
    }


    @GetMapping("get-all")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_ORDER.value) || hasRole('ADMIN')")
    fun getList(
        request: PlanSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): ResponseEntity<PagingEquipmentProdResponse> {

        val data = equipmentProductivityService.getPaginatedEquipmentProductivityPlan(request, pageable)
        return ResponseEntity<PagingEquipmentProdResponse>(data, HttpStatus.OK)
    }


    @PostMapping(value = ["import-excel"], consumes = ["multipart/form-data"])
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun importExcel(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = equipmentProductivityService.importExcel(file)
        return ResponseEntity(data, HttpStatus.OK)
    }
}