package com.kcvn.spm.app.productprocess.controller

import com.kcvn.spm.app.auth.payload.request.RoleRequest
import com.kcvn.spm.app.productprocess.payload.request.ProductProcessSearchRequest
import com.kcvn.spm.app.productprocess.payload.request.UpdateProductProcessDetailRequest
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ProductProcess
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
        @PageableDefault(size = 10, page = 0, sort = ["productName,asc","layerCode,asc","processCode,asc"]) pageable: Pageable?
    ): ResponseEntity<BasePagingResponse<ProductProcessResponse>>
    {
        val result = productProcessService.getPaginatedProductProcess(request.search,request.hasProcessConvertCode, pageable!!);
       if(result.data.isNullOrEmpty()){
           return  ResponseEntity(BasePagingResponse(),HttpStatus.NO_CONTENT)
       }
        else{
            return  ResponseEntity(result,HttpStatus.OK)
        }
    }

    @PutMapping("/update-product-process-detail")
    fun updateProductProcess(
        @Valid @RequestBody request: UpdateProductProcessDetailRequest
    ): ResponseEntity<BaseResponse<List<ProductProcess?>>> {
        val productProcess = productProcessService.updateProductProcessDetail(request)
        return ResponseEntity(BaseResponse(data = productProcess, message = CommonUtils.getMessage("update.succeeded")),HttpStatus.OK)
    }

}