package com.kcvn.spm.app.completionrate.controller

import com.kcvn.spm.app.completionrate.payload.request.CompletionRateProcessProductRequest
import com.kcvn.spm.app.completionrate.payload.request.CompletionRateSearchRequest
import com.kcvn.spm.app.completionrate.service.CompletionRateService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/completion-rate")
class CompletionRateController(
    private val completionRateService: CompletionRateService
) {


    @GetMapping("/download-template-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = completionRateService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    //Product Function



    @GetMapping("/product/export-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun exportCompletionRateProductExcel(
            request: CompletionRateSearchRequest?,
            @PageableDefault(size = 100000, page = 0)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["productname"], direction = Sort.Direction.ASC)
        )
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = completionRateService.exportCompletionRateProductExcel(
            request?.search,
            pageable
        )
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/product/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun getAllCompletionRateProducts(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 100000, page = 0)
            @SortDefault.SortDefaults(
            SortDefault(sort = ["productname"], direction = Sort.Direction.ASC)
            )
            pageable: Pageable
    ): ResponseEntity<PaginatedResponse> {
        val result = completionRateService.getPaginatedCompletionRateProduct(search, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping(value = ["/product/import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun importExcel(@RequestPart("file") file: MultipartFile,
                    @RequestParam("effectivedate") effectiveDate: OffsetDateTime,): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = completionRateService.importExcelCompletionRateProduct(file,effectiveDate)
        return ResponseEntity(data, HttpStatus.OK)
    }


    //Process Product Function
    @GetMapping("/process-product/export-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun exportCompletionRateProcessProductExcel(
            request: CompletionRateProcessProductRequest?,
            @PageableDefault(size = 100000, page = 0)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["product_name_shortcut"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["layerCode"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["process_code"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = completionRateService.exportCompletionRateProcessProductExcel(
            request,
            pageable
        )
        return ResponseEntity(data, HttpStatus.OK)
    }


    @GetMapping("/process-product/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun getAllCompletionRateProcessProducts(
            search: CompletionRateProcessProductRequest?,
            @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<PaginatedResponse> {
        val result =
            completionRateService.getPaginatedCompletionRateProcessesProduct(search, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }


    @PostMapping(value = ["/process-product/import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun importExcelCompletionProcessProduct(@RequestPart("file") file: MultipartFile,
                    @RequestParam("effectivedate") effectiveDate: OffsetDateTime,
                    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = completionRateService.importExcelProcessProduct(file,effectiveDate)
        return ResponseEntity(data, HttpStatus.OK)
    }
    //Process Function


    @PostMapping(value = ["/process/import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun importExcelCompletionProcess(@RequestPart("file") file: MultipartFile,
        @RequestParam("effectivedate") effectiveDate: OffsetDateTime,
        ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = completionRateService.importExcelCompletionRateProcess(file,effectiveDate)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/process/export-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun exportCompletionRateProcessExcel(
            request: CompletionRateSearchRequest?,
            @PageableDefault(size = 1000000, page = 0)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["processCode"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["layerCode"], direction = Sort.Direction.ASC),

            )
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = completionRateService.exportCompletionRateProcessExcel(
            request?.search,
            pageable
        )
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/process/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun getAllProcess(
        @RequestParam(required = false) search: String?,
        @SortDefault.SortDefaults(
            SortDefault(sort = ["processCode"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["layerCode"], direction = Sort.Direction.ASC),

            )
        pageable: Pageable
    ): ResponseEntity<PaginatedResponse> {
        val result = completionRateService.getPaginatedCompletionRateProcesses(search, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }



}

