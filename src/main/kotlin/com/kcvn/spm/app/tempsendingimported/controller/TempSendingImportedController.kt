package com.kcvn.spm.app.tempsendingimported.controller

import com.kcvn.spm.app.tempsendingimported.payload.response.TempSendingImportedResponse
import com.kcvn.spm.app.tempsendingimported.service.TempSendingImportedService
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/temp-sending-imported")
class TempSendingImportedController(private val tempSendingImportedService: TempSendingImportedService) {
    @GetMapping("form-code-dropdown")
    fun getListFormCodeDropdown(@RequestParam isIncludeApproved: Boolean, isIncludeGe3Days: Boolean? = true): ResponseEntity<BaseResponse<List<DropdownResponse>>> {
        val data = tempSendingImportedService.getListFormCodeDropdown(isIncludeApproved, isIncludeGe3Days ?: false)
        return ResponseEntity<BaseResponse<List<DropdownResponse>>>(data, HttpStatus.OK)
    }

    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(
        @RequestParam(required = false) formCode: String
    ): ResponseEntity<BasePagingResponse<TempSendingImportedResponse>> {
        val result = tempSendingImportedService.getList(formCode)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping(value = ["import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun importTxtSending(
        @RequestParam("formCode") formCode: String,
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<BaseResponse<Int>> {
        val data = tempSendingImportedService.importTxtSending(formCode, file)
        return ResponseEntity(data, HttpStatus.OK)
    }
}