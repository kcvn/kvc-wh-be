package com.kcvn.spm.app.completionrate.controller

import com.kcvn.spm.app.completionrate.payload.request.CompletionRateSearchRequest
import com.kcvn.spm.app.completionrate.service.CompletionRateService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.LocalDateTime

@RestController
@RequestMapping("/api/completion-rate")
class CompletionRateController(
    private val completionRateService: CompletionRateService
) {
    //Product Function



    @GetMapping("/product/export-excel")
    fun exportCompletionRateProductExcel(
        request: CompletionRateSearchRequest,
        @PageableDefault(size = 1000000, page = 0)
        @SortDefault.SortDefaults(
//            SortDefault(sort = ["product_name"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = completionRateService.exportCompletionRateProductExcel(
            request.search,
            pageable
        )
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/product/all")
    fun getAllCompletionRateProducts(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<PaginatedResponse> {
        val result = completionRateService.getPaginatedCompletionRateProduct(search, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping(value = ["/product/import-csv"], consumes = ["multipart/form-data"])
    fun importCsvCompletionRateProducts(@RequestPart("file") multipartFile: MultipartFile): ResponseEntity<*> {
        return try {
            val data = completionRateService.importCsvCompletionRateProduct(multipartFile)
            ResponseEntity<MessageResponse>(
                MessageResponse(data),
                HttpStatus.OK
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("import.failed")),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }


    //Process Product Function
    @GetMapping("/process-product/export-excel")
    fun exportCompletionRateProcessProductExcel(
        request: CompletionRateSearchRequest,
        @PageableDefault(size = 1000000, page = 0)
        @SortDefault.SortDefaults(
//            SortDefault(sort = ["product_name"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = completionRateService.exportCompletionRateProcessProductExcel(
            request.search,
            pageable
        )
        return ResponseEntity(data, HttpStatus.OK)
    }


    @GetMapping("/process-product/all")
    fun getAllCompletionRateProcessProducts(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<PaginatedResponse> {
        val result =
            completionRateService.getPaginatedCompletionRateProcessesProduct(search, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }


    @PostMapping(value = ["/process-product/import-csv"], consumes = ["multipart/form-data"])
    fun importCsvCompletionRateProcessProducts(
        @RequestPart("file") multipartFile: MultipartFile,
        @RequestParam("effectivedate") effectiveDate: LocalDateTime,
        @RequestParam("expirationdate") expirationDate: LocalDateTime?
    ): ResponseEntity<*> {
        return try {
            val data = completionRateService.importCsvProcessProduct(multipartFile, effectiveDate, expirationDate)
            ResponseEntity<MessageResponse>(
                MessageResponse(data),
                HttpStatus.OK
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("import.failed")),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

    //Process Function

    @GetMapping("/process/export-excel")
    fun exportCompletionRateProcessExcel(
        request: CompletionRateSearchRequest,
        @PageableDefault(size = 1000000, page = 0)
        @SortDefault.SortDefaults(
//            SortDefault(sort = ["product_name"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = completionRateService.exportCompletionRateProcessExcel(
            request.search,
            pageable
        )
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/process/all")
    fun getAllProcess(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 10, page = 0) pageable: Pageable
    ): ResponseEntity<PaginatedResponse> {
        val result = completionRateService.getPaginatedCompletionRateProcesses(search, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping(value = ["/process/import-csv"], consumes = ["multipart/form-data"])
    fun importCsvCompletionRateProcess(
        @RequestPart("file") multipartFile: MultipartFile,
        @RequestParam("effectivedate") effectiveDate: LocalDateTime,
        @RequestParam("expirationdate") expirationDate: LocalDateTime?
    ): ResponseEntity<*> {
        return try {
            val data = completionRateService.importCsvProcess(multipartFile, effectiveDate, expirationDate)
            ResponseEntity<MessageResponse>(
                MessageResponse(data),
                HttpStatus.OK
            )
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("import.failed")),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }

}

