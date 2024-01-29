package com.kcvn.spm.app.product.controller

import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.product.payload.response.ProductResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.FileResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@RestController
@RequestMapping("/api/product")
class ProductController {
    @GetMapping("/get-list")
    fun getList(
        request: ProductSearchRequest?,
        @PageableDefault(size = 10, page = 0) pageable: Pageable?
    ): ResponseEntity<PagingProductResponse> {
        return try {
            ResponseEntity<PagingProductResponse>(null, HttpStatus.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<PagingProductResponse>(null, HttpStatus.OK)
        }
    }

    @PostMapping(value = ["/import-csv"], consumes = ["multipart/form-data"])
    fun importCsv(@RequestPart("file") multipartFile: MultipartFile): ResponseEntity<*> {
        return ResponseEntity<FileResponse>(null, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    fun exportExcel(request: ProductSearchRequest?): StreamingResponseBody? {
        return null
    }

    @PostMapping("/sync")
    fun sync(): ResponseEntity<*> {
        return ResponseEntity<Any?>(null, HttpStatus.OK)
    }
}