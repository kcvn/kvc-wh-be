package com.kcvn.spm.app.backlogwh.controller

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhUpdateRequest
import com.kcvn.spm.app.backlogwh.payload.request.ImportBacklogWh
import com.kcvn.spm.app.backlogwh.payload.response.BacklogHistoryResponse
import com.kcvn.spm.app.backlogwh.payload.response.BacklogWhResponse
import com.kcvn.spm.app.backlogwh.service.BacklogWhService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import jakarta.validation.Valid
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
@RequestMapping("/api/backlog-wh")
class BacklogWhController(private val backlogWhService: BacklogWhService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(
        request: BacklogWhSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["createdDate"], direction = Sort.Direction.DESC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<BacklogWhResponse>> {
        val result = backlogWhService.getList(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/get-list-for-android")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getListForAndroid(
        request: BacklogWhSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["createdDate"], direction = Sort.Direction.DESC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<BacklogWhResponse>> {
        val result = backlogWhService.getListForAndroid(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @GetMapping("/get-backlog-history")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getBacklogHistory(
        packageCode: String,
    ): ResponseEntity<BasePagingResponse<BacklogHistoryResponse>> {
        val result = backlogWhService.getBacklogHistoryList(packageCode)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PutMapping("/update")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun update(
        @Valid @RequestBody request: List<BacklogWhUpdateRequest>
    ): ResponseEntity<*> {
        backlogWhService.update(request)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded")),
            HttpStatus.OK
        )
    }

    @GetMapping("/download-template-bin-entry")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun downloadTemplateBinEntry(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = backlogWhService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @PostMapping(value = ["import-bin-entry"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun importBinEntry(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<BaseResponse<List<ImportBacklogWh>>> {
        val data = backlogWhService.importBinEntry(file)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/export-bin-entry")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_ORDER.value) || hasRole('ADMIN')")
    fun exportBinEntry(
        @PageableDefault(size = PagingDefault.EXPORT_SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = backlogWhService.exportBinEntry(pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/export-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_ORDER.value) || hasRole('ADMIN')")
    fun exportExcel(
        request: BacklogWhSearchRequest,
        @PageableDefault(size = PagingDefault.EXPORT_SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = backlogWhService.exportBacklogWhExcel(request, pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }
}