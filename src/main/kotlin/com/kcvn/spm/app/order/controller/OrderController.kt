package com.kcvn.spm.app.order.controller

import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.payload.response.PagingOrderResponse
import com.kcvn.spm.app.order.service.OrderService
import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.PagingProductResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.repository.OrderDetailRepository
import com.kcvn.spm.repository.OrderRepository
import org.springframework.data.domain.PageRequest
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/order")
class OrderController (private val orderService: OrderService,
) {


    @GetMapping("/get-list")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(
        request: OrderSearchRequest?,
        @PageableDefault(size = 10, page = 0)
//        @SortDefault.SortDefaults(SortDefault(sort = ["createddate"], direction = Sort.Direction.DESC))
        pageable: Pageable
    ): ResponseEntity<PagingOrderResponse> {
        val data = orderService.getPaginatedCompletionRateProduct(request,pageable)
        return ResponseEntity<PagingOrderResponse>(data, HttpStatus.OK)
    }

    @PostMapping(value = ["/import-excel"], consumes = ["multipart/form-data"])
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun importExcel(@RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = BaseResponse<FileContentModel>(data = null, message = "Import file thành công")
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template-excel")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_PRODUCT.value) || hasRole('ADMIN')")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = BaseResponse<FileContentModel>(data = null, message = "Download file thành công")
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_PRODUCT.value) || hasRole('ADMIN')")
    fun exportExcel(
        request: OrderSearchRequest?,
        @PageableDefault(size = 1000000, page = 0)
        @SortDefault.SortDefaults(SortDefault(sort = ["createddate"], direction = Sort.Direction.DESC))
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = BaseResponse<FileContentModel>(data = null, message = "Export file thành công")
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/list-order-code-by-year")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_PRODUCT.value) || hasRole('ADMIN')")
    fun getListOrderCode(year: String): ResponseEntity<BaseResponse<List<DropdownResponse>>> {
        val check = mutableListOf<DropdownResponse>()
        check.add(DropdownResponse("1", "Label 1"))
        check.add(DropdownResponse("2", "Label 2"))
        val data = BaseResponse<List<DropdownResponse>>(data = check, message = "Lấy mã đơn hàng thành công")

        return ResponseEntity(data, HttpStatus.OK)
    }
    @GetMapping("/list-version-by-order-code")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_PRODUCT.value) || hasRole('ADMIN')")
    fun getListVersion(orderCode: String): ResponseEntity<BaseResponse<List<DropdownResponse>>> {
        val check = mutableListOf<DropdownResponse>()
        check.add(DropdownResponse("1", "Label 1"))
        check.add(DropdownResponse("2", "Label 2"))
        val data = BaseResponse<List<DropdownResponse>>(data = check, message = "Lấy version thành công")
        return ResponseEntity(data, HttpStatus.OK)
    }
}