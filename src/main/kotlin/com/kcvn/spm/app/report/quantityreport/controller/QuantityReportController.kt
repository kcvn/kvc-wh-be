package com.kcvn.spm.app.report.quantityreport.controller

import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.app.report.quantityreport.payload.request.QuantityReportRequest
import com.kcvn.spm.app.report.quantityreport.payload.response.CheckCalculateQuantityResponse
import com.kcvn.spm.app.report.quantityreport.payload.response.PagingQuantityReportResponse
import com.kcvn.spm.app.report.quantityreport.service.QuantityReportService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CalculateQuantityResult
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/report/quantity-report")
class QuantityReportController(
    private val quantityReportService: QuantityReportService,
) {
    @PostMapping("/calculate-quantity")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CA_REPORT_QUANTITY.value) || hasRole('ADMIN')")
    fun CalculateQuantity(@RequestBody request: CalculateQuantityRequest): ResponseEntity<BaseResponse<FileContentModel?>> {
        val data = quantityReportService.calculateQuantity(request)
        if(data.data != null){
            return ResponseEntity(data, HttpStatus.BAD_REQUEST)
        }
        
        return ResponseEntity<BaseResponse<FileContentModel?>>(data, HttpStatus.OK)
    }

    @GetMapping("/locked-quantity")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).LOCK_REPORT_QUANTITY.value) || hasRole('ADMIN')")
    fun LockedQuantity(id: String): ResponseEntity<BaseResponse<Boolean>> {
        val result = quantityReportService.lockedQuantity(id)
        return ResponseEntity<BaseResponse<Boolean>>(result, HttpStatus.OK)
    }

    @GetMapping("/get-list-calculate-quantity-result")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_REPORT_QUANTITY.value) || hasRole('ADMIN')")
    fun getListCalculateQuantityResult(
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["yearNumber"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["monthNumber"], direction = Sort.Direction.DESC),

        ) pageable: Pageable,
    ): ResponseEntity<BasePagingResponse<CalculateQuantityResult>> {
        val result = quantityReportService.getListCalculateQuantityResult(pageable)
        return ResponseEntity<BasePagingResponse<CalculateQuantityResult>>(result, HttpStatus.OK)
    }

    @GetMapping("/get-list-quantity-report")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_REPORT_QUANTITY.value) || hasRole('ADMIN')")
    fun getListQuantityReport(
        request: QuantityReportRequest?,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["yearNumber"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["monthNumber"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["productName"], direction = Sort.Direction.ASC),
        ) pageable: Pageable,
    ) : ResponseEntity<PagingQuantityReportResponse>{
        val data = quantityReportService.getListQuantityReport(request,pageable)
        return ResponseEntity<PagingQuantityReportResponse>(data,HttpStatus.OK)
    }

    @GetMapping("/check-quantity-report")
    fun checkQuantityReport(request: CalculateQuantityRequest
    ) : ResponseEntity<BaseResponse<CheckCalculateQuantityResponse>>{
        val data = quantityReportService.checkCalculateQuantity(request)

        val month = request.monthReport
        val year = request.yearReport
        return if(data.hasCalculateQuantity ){
            ResponseEntity(
                BaseResponse(data = data, message = CommonUtils.getMessage("quantity.validation",arrayOf(month.toString(), year.toString()))),
                HttpStatus.OK
            )
        }else{
            ResponseEntity(
                BaseResponse(data = data, message = "Ok"),
                HttpStatus.OK
            )
        }
    }

    @GetMapping("/export-excel")
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_REPORT_QUANTITY.value) || hasRole('ADMIN')")
    fun exportQuantityReportExcel(
        request: QuantityReportRequest?,
        @PageableDefault(size = PagingDefault.EXPORT_SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["productName"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["yearNumber"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["monthNumber"], direction = Sort.Direction.DESC),
        ) pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = quantityReportService.exportQuantityReportExcel(request, pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }
}