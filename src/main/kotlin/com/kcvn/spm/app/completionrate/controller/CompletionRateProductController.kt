package com.kcvn.spm.app.completionrate.controller

import com.kcvn.spm.app.completionrate.service.completionRateProductService
import com.kcvn.spm.common.payload.PaginatedResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

class CompletionRateProductController {
    @RestController
    @RequestMapping("/api/completion-rate/product")
    class CompletionRateProductController(private val completionRateProductService: completionRateProductService) {

        @GetMapping("/all")
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







    }
}
