package com.kcvn.spm.app.tempcheckingimported.controller

import com.kcvn.spm.app.tempcheckingimported.payload.response.TempCheckingImportedResponse
import com.kcvn.spm.app.tempcheckingimported.service.TempCheckingImportedService
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.opencsv.CSVWriter
import com.opencsv.bean.StatefulBeanToCsvBuilder
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/temp-checking-imported")
class TempCheckingImportedController(private val tempCheckingImportedService: TempCheckingImportedService) {
    @GetMapping("form-code-dropdown")
    fun getListFormCodeDropdown(@RequestParam formStatus: String? = "ALL", isIncludeGe3Days: Boolean?): ResponseEntity<BaseResponse<List<DropdownResponse>>> {
        val data = tempCheckingImportedService.getListFormCodeDropdown(formStatus ?: "ALL", isIncludeGe3Days ?: false)
        return ResponseEntity<BaseResponse<List<DropdownResponse>>>(data, HttpStatus.OK)
    }

    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(
        @RequestParam(required = false) formCode: String
    ): ResponseEntity<BasePagingResponse<TempCheckingImportedResponse>> {
        val result = tempCheckingImportedService.getList(formCode)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/get-list-by-time-range")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getListByTimeRange(
        @RequestParam(required = false) formCode: String
    ): ResponseEntity<BasePagingResponse<TempCheckingImportedResponse>> {
        val result = tempCheckingImportedService.getList(formCode)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping(value = ["import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun importExcelChecking(
        @RequestParam("formCode") formCode: String,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<BaseResponse<Int>> {
        val data = tempCheckingImportedService.importChecking(formCode, file)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun downloadTemplate(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = tempCheckingImportedService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }




}