package com.kcvn.spm.app.equipmentproductivity.controller

import com.kcvn.spm.app.equipmentproductivity.service.EquipmentProductivityService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/machine-productivity")
class EquipmentProductivityController(
    private val equipmentProductivityService: EquipmentProductivityService)
{
    @PostMapping(value = ["/product/import-excel"], consumes = ["multipart/form-data"])
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun importExcel(
        @RequestPart("file") file: MultipartFile,
        @RequestParam("startDate") startDate: OffsetDateTime,
        @RequestParam("endDate") endDate: OffsetDateTime
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = equipmentProductivityService.importExcel(file, startDate,endDate)
        return ResponseEntity(data, HttpStatus.OK)
    }
}