package com.kcvn.spm.app.workresult.controller

import com.kcvn.spm.app.workresult.payload.request.WorkResultSearchRequest
import com.kcvn.spm.app.workresult.payload.response.ProcessGroupResponse
import com.kcvn.spm.app.workresult.payload.response.ProcessResponse
import com.kcvn.spm.app.workresult.payload.response.WorkResultResponse
import com.kcvn.spm.app.workresult.service.WorkResultService
import com.kcvn.spm.common.payload.BasePagingResponse
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
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/work-result")
class WorkResultController (
    private val workResultService: WorkResultService
) {
    @GetMapping("/get-list-work-result")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_WORK_RESULT.value) || hasRole('ADMIN')")
    fun getListWorkResult(
        request: WorkResultSearchRequest?,
        @PageableDefault(size = 10, page = 0)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["summary_result_date"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["item_name"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["layer_code"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["process_name"], direction = Sort.Direction.ASC)
        ) pageable: Pageable,
    ): ResponseEntity<BasePagingResponse<WorkResultResponse>> {
        val data = workResultService.getListWorkResult(request,pageable)
        return ResponseEntity<BasePagingResponse<WorkResultResponse>>(data, HttpStatus.OK)
    }

    @GetMapping("get-list-process-groups")
    fun getListProcessGroups() : ResponseEntity<BaseResponse<List<DropdownResponse>>>{
        val data = workResultService.getListProcessGroup()
        return ResponseEntity<BaseResponse<List<DropdownResponse>>>(data, HttpStatus.OK)
    }

    @GetMapping("get-list-process-by-group-code")
    fun getListProcessByGroupCode(groupCodes: String) : ResponseEntity<BaseResponse<List<DropdownResponse>>>{
        val data = workResultService.getListProcessByGroupCode(groupCodes)
        return ResponseEntity<BaseResponse<List<DropdownResponse>>>(data, HttpStatus.OK)
    }

    @GetMapping("export-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_WORK_RESULT.value) || hasRole('ADMIN')")
    fun exportExcel(
        request: WorkResultSearchRequest?,
        @PageableDefault(size = 1000000, page = 0)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["summary_result_date"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["item_name"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["layer_code"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["process_name"], direction = Sort.Direction.ASC)
        ) pageable: Pageable,
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = workResultService.exportExcel(request,pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @PostMapping("/export-excel-to-download")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_WORK_RESULT.value) || hasRole('ADMIN')")
    fun generateXlsReport(
        request: WorkResultSearchRequest?,
        @PageableDefault(size = 1000000, page = 0) pageable: Pageable
    ): ResponseEntity<ByteArray> {
        val report = workResultService.exportExcel(request, pageable)

        return workResultService.createResponseEntity(report.data?.content, report.data?.fileName)
    }

}