package com.kcvn.spm.app.purchaseOrderBacklog.controller

import com.kcvn.spm.app.purchaseOrderBacklog.service.PurchaseOrderBacklogService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/purchase-order-backlog")
class PurchaseOrderBacklogController(private val purchaseOrderBacklogService: PurchaseOrderBacklogService) {

    @PostMapping(value = ["import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun importExcelChecking(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<BaseResponse<Int>> {
        val data = purchaseOrderBacklogService.importPurchaseOrderBacklog(file)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun downloadTemplate(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = purchaseOrderBacklogService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }
}