package com.kcvn.spm.app.tempcheckingimported.controller

import com.kcvn.spm.app.tempcheckingimported.payload.response.TempCheckingImportedResponse
import com.kcvn.spm.app.tempcheckingimported.service.TempCheckingImportedService
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/temp-checking-imported")
class TempCheckingImportedController(private val tempCheckingImportedService: TempCheckingImportedService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(): ResponseEntity<BasePagingResponse<TempCheckingImportedResponse>> {
        val result = tempCheckingImportedService.getList()
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping(value = ["import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasRole('ADMIN')")
    fun importExcelChecking(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<BaseResponse<Int>> {
        val data = tempCheckingImportedService.importChecking(file)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun downloadTemplate(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = tempCheckingImportedService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }
}