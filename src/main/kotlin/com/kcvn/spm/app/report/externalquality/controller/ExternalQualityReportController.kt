package com.kcvn.spm.app.report.externalquality.controller

import com.kcvn.spm.app.report.externalquality.payload.request.ExternalQualityReportSearchRequest
import com.kcvn.spm.app.report.externalquality.payload.response.ExternalQualityReportResponse
import com.kcvn.spm.app.report.externalquality.service.ExternalQualityReportService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/report/external-quality")
class ExternalQualityReportController (private val externalQualityReportService: ExternalQualityReportService) {

    @GetMapping("")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_ORDER.value) || hasRole('ADMIN')")
    fun getData(
        request: ExternalQualityReportSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE) pageable: Pageable
    ): ResponseEntity<ExternalQualityReportResponse> {
        val data = externalQualityReportService.getDataReport(request, pageable)
        return ResponseEntity<ExternalQualityReportResponse>(data, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    //@PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_ORDER.value) || hasRole('ADMIN')")
    fun exportExcel(
        request: ExternalQualityReportSearchRequest
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = BaseResponse<FileContentModel>()
        return ResponseEntity<BaseResponse<FileContentModel>>(data, HttpStatus.OK)
    }
}