package com.kcvn.spm.app.order.controller

import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.payload.response.OrderCodeResponse
import com.kcvn.spm.app.order.payload.response.PagingOrderResponse
import com.kcvn.spm.app.order.service.OrderService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/order")
class OrderController(
    private val orderService: OrderService
) {


    @GetMapping("/get-list")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(
        request: OrderSearchRequest?,
        @PageableDefault(size = 10, page = 0)
//        @SortDefault.SortDefaults(SortDefault(sort = ["createddate"], direction = Sort.Direction.DESC))
        pageable: Pageable
    ): ResponseEntity<PagingOrderResponse> {
        val data = orderService.getPaginatedOrder(request, pageable)
        return ResponseEntity<PagingOrderResponse>(data, HttpStatus.OK)
    }

    @PostMapping(value = ["/import-excel"], consumes = ["multipart/form-data"])
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun importExcel(orderCode: String?, @RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = orderService.importExcelOrder(file, orderCode)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template-excel")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_PRODUCT.value) || hasRole('ADMIN')")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = orderService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_PRODUCT.value) || hasRole('ADMIN')")
    fun exportExcel(
        request: OrderSearchRequest?,
        @PageableDefault(size = 1000000, page = 0)
//        @SortDefault.SortDefaults(SortDefault(sort = ["createddate"], direction = Sort.Direction.DESC))
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = orderService.exportOrderExcel(request, pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/order-code-dropdown")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_PRODUCT.value) || hasRole('ADMIN')")
    fun getListOrderCode(year: String?): ResponseEntity<BaseResponse<List<OrderCodeResponse>>> {
        val result = orderService.getOrderCode(year)
        val data = BaseResponse(result)
        return ResponseEntity(data, HttpStatus.OK)
    }
}