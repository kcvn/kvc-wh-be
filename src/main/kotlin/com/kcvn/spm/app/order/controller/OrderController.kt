package com.kcvn.spm.app.order.controller

import com.kcvn.spm.app.order.payload.model.CheckWorkResultModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.payload.response.OrderCodeResponse
import com.kcvn.spm.app.order.payload.response.PagingOrderResponse
import com.kcvn.spm.app.order.service.OrderService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestPart
import org.springframework.web.bind.annotation.RestController
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/order")
class OrderController(
    private val orderService: OrderService
) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_ORDER.value) || hasRole('ADMIN')")
    fun getList(
        request: OrderSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["productName"], direction = Sort.Direction.ASC)
        )
        pageable: Pageable
    ): ResponseEntity<PagingOrderResponse> {
        val data = orderService.getPaginatedOrder(request, pageable)
        return ResponseEntity<PagingOrderResponse>(data, HttpStatus.OK)
    }

    @PostMapping(value = ["/import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_ORDER.value) || hasRole('ADMIN')")
    fun importExcel(increaseVersion: Boolean?, @RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = orderService.importExcelOrder(file, increaseVersion)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_ORDER.value) || hasRole('ADMIN')")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = orderService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_ORDER.value) || hasRole('ADMIN')")
    fun exportExcel(
        request: OrderSearchRequest,
        @PageableDefault(size = PagingDefault.EXPORT_SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["productName"], direction = Sort.Direction.ASC)
        )
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = orderService.exportOrderExcel(request, pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/order-code-dropdown")
    fun getListOrderCode(year: String?): ResponseEntity<BaseResponse<List<OrderCodeResponse>>> {
        val result = orderService.getOrderCode(year)
        val data = BaseResponse(result)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/version-dropdown")
    fun getVersionDropDown(): ResponseEntity<BaseResponse<List<DropdownResponse>>> {
        val data = orderService.getOrderVersionDropdown()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/check-work-result")
    fun checkWorkResult(orderCode: String): ResponseEntity<BaseResponse<CheckWorkResultModel>> {
        val data = orderService.checkWorkResult(orderCode)
        return ResponseEntity(data, HttpStatus.OK)
    }
}