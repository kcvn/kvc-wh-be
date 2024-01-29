package com.kcvn.spm.app.product.controller

import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.ProductResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.PagingResponse
import com.kcvn.spm.common.util.CommonUtils
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/product")
class ProductController {
    @GetMapping("/get-list")
    fun getListProduct(
        request: ProductSearchRequest?,
        @PageableDefault(size = 10, page = 0) pageable: Pageable?
    ): ResponseEntity<PagingResponse<ProductResponse>> {
        return try {
            //syncTransAmDataService.syncProcessProcedureStructure()
            ResponseEntity<PagingResponse<ProductResponse>>(null, HttpStatus.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<PagingResponse<ProductResponse>>(null, HttpStatus.OK)
        }
    }
}