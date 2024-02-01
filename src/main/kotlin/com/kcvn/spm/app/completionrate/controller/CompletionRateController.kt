package com.kcvn.spm.app.completionrate.controller

import com.kcvn.spm.app.completionrate.service.CompletionRateProductService
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile


@RestController
@RequestMapping("/api/completion-rate")
class CompletionRateProductController(private val completionRateProductService: CompletionRateProductService) {

    @GetMapping("/product/all")
    fun getAllProducts(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 10, page = 0) pageable: Pageable?
    ): ResponseEntity<*> {
        return try {
            val result =
                completionRateProductService.getPaginatedCompletionRateProduct(search, pageable!!)
            if (result.data.isEmpty())
                ResponseEntity<Any>(HttpStatus.NO_CONTENT)
            else
                ResponseEntity<PaginatedResponse>(result, HttpStatus.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<Any?>(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }


    @PostMapping(value = ["/product/import-csv"], consumes = ["multipart/form-data"])
    fun importCsv(@RequestPart("file") multipartFile: MultipartFile): ResponseEntity<*> {
        try {
            val data = completionRateProductService.importCsvProduct(multipartFile)
            return ResponseEntity<MessageResponse>(
                MessageResponse(data),
                HttpStatus.OK
            )
        }
        catch (e: Exception) {
            e.printStackTrace()
            return ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("import.failed")),
                HttpStatus.INTERNAL_SERVER_ERROR
            )
        }
    }






}

