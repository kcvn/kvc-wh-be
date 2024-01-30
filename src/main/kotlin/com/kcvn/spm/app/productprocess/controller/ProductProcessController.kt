package com.kcvn.spm.app.productprocess.controller

import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.sample.service.ProductProcessService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/product-process")
class ProductProcessController(
    private  val productProcessService: ProductProcessService
)
{
    @GetMapping("/all")
    fun getAllProductProcess (
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 10, page = 0) pageable: Pageable?
    ): ResponseEntity<*>
    {
        return try {
            val result = productProcessService.getPaginatedProductProcess(search, pageable!!);
            if(result.data.isEmpty())
                ResponseEntity<Any?>(HttpStatus.NO_CONTENT)
            else
                ResponseEntity<PaginatedResponse>(result, HttpStatus.OK)
        }  catch (e: Exception){
            e.printStackTrace()
            ResponseEntity<Any?>(e.localizedMessage, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

}