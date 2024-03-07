package com.kcvn.spm.app.machineproductivity.controller

import com.kcvn.spm.app.machineproductivity.service.MachineProductivityService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/machine-productivity")
class MachineProductivityController(
    private val machineProductivityService: MachineProductivityService)
{
    @PostMapping(value = ["/product/import-excel"], consumes = ["multipart/form-data"])
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun importExcel(
        @RequestPart("file") file: MultipartFile,
        @RequestParam("startDate") startDate: OffsetDateTime,
        @RequestParam("endDate") endDate: OffsetDateTime
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = machineProductivityService.importExcel(file, startDate,endDate)
        return ResponseEntity(data, HttpStatus.OK)
    }
}