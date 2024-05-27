package com.kcvn.spm.app.product.controller

import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.app.product.payload.response.ProductAndProcessResponse
import com.kcvn.spm.app.product.service.ProductService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.app.productprocess.service.ProductProcessService
import com.kcvn.spm.common.constants.PagingDefault
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/product")
class ProductController(
    private val productService: ProductService,
    private val productProcessService: ProductProcessService
) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(
        request: ProductSearchRequest?,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(SortDefault(sort = ["name"], direction = Sort.Direction.ASC))
        pageable: Pageable,
        httpRequest: HttpServletRequest
    ): ResponseEntity<PagingProductResponse> {
        val data = productService.getListProduct(request, pageable, httpRequest)
        return ResponseEntity<PagingProductResponse>(data, HttpStatus.OK)
    }

    @PostMapping(value = ["/import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun importExcel(@RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = productService.importExcelProduct(file)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_PRODUCT.value) || hasRole('ADMIN')")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = productService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_PRODUCT.value) || hasRole('ADMIN')")
    fun exportExcel(
        request: ProductSearchRequest?,
        @PageableDefault(size = PagingDefault.EXPORT_SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(SortDefault(sort = ["createddate"], direction = Sort.Direction.DESC))
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = productService.exportExcel(request, pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/get-product-detail/{name}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getProductDetail(@PathVariable("name") nameProduct: String?): ResponseEntity<BaseResponse<ProductAndProcessResponse>> {
        val dataProduct = productService.getProductDetail(nameProduct)
        val dataProcess = productProcessService.getProductProcessDetail(nameProduct)

        val resultData = ProductAndProcessResponse(
            detail = dataProduct,
            listProcess = dataProcess
        )
        return if (resultData.detail != null || resultData.listProcess != null) {
            ResponseEntity(
                BaseResponse(data = resultData, message = CommonUtils.getMessage("data.success")),
                HttpStatus.OK
            )
        } else {
            ResponseEntity(BaseResponse(message = CommonUtils.getMessage("data.notFound")), HttpStatus.NOT_FOUND)
        }
    }
}