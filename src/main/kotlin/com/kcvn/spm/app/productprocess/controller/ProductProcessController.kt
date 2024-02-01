package com.kcvn.spm.app.productprocess.controller

import com.kcvn.spm.app.auth.payload.request.RoleRequest
import com.kcvn.spm.app.productprocess.payload.request.ProductProcessSearchRequest
import com.kcvn.spm.app.productprocess.payload.request.UpdateProductProcessDetailRequest
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.sample.service.ProductProcessService
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/product-process")
class ProductProcessController(
    private  val productProcessService: ProductProcessService
)
{
    @GetMapping("/all")
    fun getAllProductProcess (
        request: ProductProcessSearchRequest,
        @PageableDefault(size = 10, page = 0) pageable: Pageable?
    ): ResponseEntity<*>
    {
        return try {
            val result = productProcessService.getPaginatedProductProcess(request.search,request.hasProcessConvertCode, pageable!!);
            if(result.data.isEmpty())
                ResponseEntity<Any?>(HttpStatus.NO_CONTENT)
            else
                ResponseEntity<PaginatedResponse>(result, HttpStatus.OK)
        }  catch (e: Exception){
            e.printStackTrace()
            ResponseEntity<Any?>(e.localizedMessage, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PutMapping("/update-product-process-detail")
    fun updateRole(
        @Valid @RequestBody request: UpdateProductProcessDetailRequest
    ): ResponseEntity<*> {
        val productProcess = productProcessService.updateProductProcessDetail(request)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("update.succeeded"), productProcess),
            HttpStatus.OK
        )
    }

}