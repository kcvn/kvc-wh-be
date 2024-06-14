package com.kcvn.spm.app.inventoryproduct.controller

import com.kcvn.spm.app.inventoryproduct.payload.request.InventorySemiProductSearchRequest
import com.kcvn.spm.app.inventoryproduct.payload.response.InventorySemiProductResponse
import com.kcvn.spm.app.inventoryproduct.service.InventorySemiProductService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/inventory-semi-product")
class InventorySemiProductController(
    private val inventorySemiProductService: InventorySemiProductService
) {

    @GetMapping("/check-inventory")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun checkInventory(date: OffsetDateTime): ResponseEntity<BaseResponse<Boolean>> {
        val data = inventorySemiProductService.checkInventory(date)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template-excel")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = inventorySemiProductService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @PostMapping(value = ["/import-excel"], consumes = ["multipart/form-data"])
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun importExcel(date: OffsetDateTime, @RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = inventorySemiProductService.importInventory(date, file)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_INVENTORY.value) || hasRole('ADMIN')")
    fun getList(
        request: InventorySemiProductSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["inventoryDate"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["productName"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<InventorySemiProductResponse>> {
        val result = inventorySemiProductService.getList(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_INVENTORY.value) || hasRole('ADMIN')")
    fun exportExcel(
        request: InventorySemiProductSearchRequest,
        @PageableDefault(size = PagingDefault.EXPORT_SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["inventoryDate"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["productName"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = inventorySemiProductService.exportExcel(request, pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }
}