package com.kcvn.spm.app.product.controller

import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.product.payload.response.ProductAndProcessResponse
import com.kcvn.spm.app.product.service.ProductService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.servlet.http.HttpServletResponse
import com.kcvn.spm.sample.service.ProductProcessService
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody
import java.io.File
import java.nio.file.Files

@RestController
@RequestMapping("/api/product")
class ProductController(
    private val productService: ProductService,
    private val productProcessService: ProductProcessService
) {
    @GetMapping("/get-list")
    fun getList(
        request: ProductSearchRequest?,
        @PageableDefault(size = 10, page = 0, sort = ["createddate,desc"]) pageable: Pageable
    ): ResponseEntity<PagingProductResponse> {
        return try {
            val data = productService.getListProduct(request, pageable)
            ResponseEntity<PagingProductResponse>(data, HttpStatus.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<PagingProductResponse>(null, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @PostMapping(value = ["/import-excel"], consumes = ["multipart/form-data"])
    fun importCsv(@RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = productService.importExcelProduct(file)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template-excel")
    fun downloadTemplateExcel() : ResponseEntity<BaseResponse<FileContentModel>> {
        val data = productService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    fun exportExcel(
        request: ProductSearchRequest?,
        @PageableDefault(size = 1000000, page = 0, sort = ["createddate,desc"]) pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = productService.exportExcel(request, pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @PostMapping("/sync")
    fun sync(): ResponseEntity<*> {
        return ResponseEntity<Any?>(null, HttpStatus.OK)
    }

    @GetMapping("/get-product-detail/{id}")
    fun getProductDetail(@PathVariable("id") id: String): ResponseEntity<BaseResponse<ProductAndProcessResponse>> {
        val dataProduct = productService.getProductDetail(id)
        val nameProduct = dataProduct?.name
        val dataProcess = productProcessService.getProductProcessDetail(nameProduct)
        dataProcess?.forEachIndexed{idx, data ->
            data?.idx = idx
        }
        val resultData = ProductAndProcessResponse(
            detail = dataProduct,
            listProcess = dataProcess
        )
        return if (resultData.detail != null ) {
            ResponseEntity(BaseResponse(data = resultData, message = CommonUtils.getMessage("data.success")), HttpStatus.OK)
        } else {
            ResponseEntity(BaseResponse(message = CommonUtils.getMessage("data.notFound")), HttpStatus.NOT_FOUND)
        }
    }
}