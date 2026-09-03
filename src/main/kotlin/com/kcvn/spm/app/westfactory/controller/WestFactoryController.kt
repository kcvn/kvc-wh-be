package com.kcvn.spm.app.westfactory.controller

import com.kcvn.spm.app.westfactory.payload.response.WestFactoryResponse
import com.kcvn.spm.app.westfactory.service.WestFactoryService
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/layout/west-factory")
class WestFactoryController(private val westFactoryService: WestFactoryService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun getList(): ResponseEntity<BasePagingResponse<WestFactoryResponse>> {
        val result = westFactoryService.getList()
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping(value = ["import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun importExcel(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<BaseResponse<Int>> {
        val data = westFactoryService.importExcel(file)
        return ResponseEntity(data, HttpStatus.OK)
    }
}