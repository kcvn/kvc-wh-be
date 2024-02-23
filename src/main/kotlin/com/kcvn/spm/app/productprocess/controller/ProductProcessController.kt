package com.kcvn.spm.app.productprocess.controller

import com.kcvn.spm.app.productprocess.payload.request.ProductProcessSearchRequest
import com.kcvn.spm.app.productprocess.payload.request.UpdateProductProcessDetailRequest
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.app.productprocess.service.ProductProcessService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/product-process")
class ProductProcessController(
    private val productProcessService: ProductProcessService
) {
    @GetMapping("/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PROCESS.value) || hasRole('ADMIN')")
    fun getAllProductProcess(
        request: ProductProcessSearchRequest,
        @PageableDefault(size = 10, page = 0)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["productName"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["layerCode"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["processSequence"], direction = Sort.Direction.ASC)
        )
        pageable: Pageable?
    ): ResponseEntity<BasePagingResponse<ProductProcessResponse?>> {
        val result =
            productProcessService.getPaginatedProductProcess(request.search, request.hasProcessConvertCode, pageable!!);
            return  ResponseEntity(result, HttpStatus.OK)

    }

    @PutMapping("/update-product-process-detail")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).U_PROCESS.value) || hasRole('ADMIN')")
    fun updateProductProcess(
        @Valid @RequestBody request: UpdateProductProcessDetailRequest
    ): ResponseEntity<BaseResponse<List<ProductProcess?>>> {
        val productProcess = productProcessService.updateProductProcessDetail(request)
        return ResponseEntity(
            BaseResponse(data = productProcess, message = CommonUtils.getMessage("update.succeeded")),
            HttpStatus.OK
        )
    }

    @GetMapping("/export-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_PROCESS.value) || hasRole('ADMIN')")
    fun exportExcel(
        request: ProductProcessSearchRequest,
        @PageableDefault(size = 1000000, page = 0)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["processName"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["layerCode"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["processSequence"], direction = Sort.Direction.ASC)
        )
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = productProcessService.exportExcel(
            request.search,
            request.hasProcessConvertCode,
            pageable
        )
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template-excel")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = productProcessService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @PostMapping(value = ["/import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_PROCESS.value) || hasRole('ADMIN')")
    fun importCsv(@RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = productProcessService.importExcelProduct(file)
        return ResponseEntity(data, HttpStatus.OK)
    }
}