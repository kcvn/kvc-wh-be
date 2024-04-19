package com.kcvn.spm.app.report.externalquality.controller

import com.kcvn.spm.app.completionrate.payload.response.CheckImportResponse
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
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

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
        request: ExternalQualityReportSearchRequest,
        @PageableDefault(size = PagingDefault.EXPORT_SIZE, page = PagingDefault.PAGE) pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = externalQualityReportService.exportExcelExternalQualityReport(request,pageable)
        return ResponseEntity<BaseResponse<FileContentModel>>(data, HttpStatus.OK)
    }
    @PostMapping(value = ["/import-excel/tape-en-route"], consumes = ["multipart/form-data"])
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_ORDER.value) || hasRole('ADMIN')")
    fun importExcel(couponCode: String, @RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = externalQualityReportService.importTapeEnRoute(file, couponCode)
        return ResponseEntity(data, HttpStatus.OK)
    }
    @PostMapping(value = ["/import-excel/tape-inventory"], consumes = ["multipart/form-data"])
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_ORDER.value) || hasRole('ADMIN')")
    fun importExcelTapeInventory(stocktakingDay: OffsetDateTime, @RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = externalQualityReportService.importTapeInventory(file, stocktakingDay)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template-excel/tape-en-route")
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_ORDER.value) || hasRole('ADMIN')")
    fun downloadTemplateTapeEnRouteExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = externalQualityReportService.downloadTapeEnRouteTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/download-template-excel/tape-inventory")
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_ORDER.value) || hasRole('ADMIN')")
    fun downloadTemplateInventoryExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = externalQualityReportService.downloadTapeInventoryTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @PostMapping(value = ["/check-import/tape-en-route"], consumes = ["multipart/form-data"])
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun checkImportTapeEnRouteExcel(couponCode: String, @RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<CheckImportResponse>> {
        val result = externalQualityReportService.checkImportTapeEnRouteExcel(couponCode, file)
        return ResponseEntity(result, HttpStatus.OK)
    }
    @PostMapping(value = ["/check-import/tape-inventory"], consumes = ["multipart/form-data"])
//    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_COMPLETION_RATE.value) || hasRole('ADMIN')")
    fun checkImportTapeEnRouteExcel(stocktakingDay: OffsetDateTime, @RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<CheckImportResponse>> {
        val result = externalQualityReportService.checkImportTapeInventory(stocktakingDay, file)
        return ResponseEntity(result, HttpStatus.OK)
    }

}