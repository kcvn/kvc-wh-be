package com.kcvn.spm.app.transaction.sending.controller

import com.kcvn.spm.app.transaction.sending.payload.request.ImportSending
import com.kcvn.spm.app.transaction.sending.payload.request.SendingRequest
import com.kcvn.spm.app.transaction.sending.payload.request.SendingSearchRequest
import com.kcvn.spm.app.transaction.sending.payload.response.SendingResponse
import com.kcvn.spm.app.transaction.sending.service.SendingTransactionsService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import jakarta.validation.Valid
import mu.KotlinLogging
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
@RequestMapping("/api/sending")
class SendingTransactionsController(private val sendingService: SendingTransactionsService) {
    @GetMapping("/get-list")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).V_PRODUCT.value) || hasRole('ADMIN')")
    fun getList(
        request: SendingSearchRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["sourceLocationCode"], direction = Sort.Direction.ASC),
        )
        pageable: Pageable
    ): ResponseEntity<BasePagingResponse<SendingResponse>> {
        val result = sendingService.getList(request, pageable)
        return ResponseEntity(result, HttpStatus.OK)
    }

    @PostMapping("/create/sending-trans")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun createSendTrans(@Valid @RequestBody request: List<SendingRequest>?): ResponseEntity<*> {
        val response = sendingService.validateSourceBacklogFromSending(request!!)
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: post sending/create/sending-trans" + ", REQUEST: " + request
        )

        return if (response.isNotEmpty()) {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("Không đủ tồn kho"), response),
                HttpStatus.OK
            )
        } else {
            sendingService.saveSendTrans(request)
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.succeeded")),
                HttpStatus.CREATED
            )
        }
    }

    @GetMapping("/export-excel")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).E_ORDER.value) || hasRole('ADMIN')")
    fun exportExcel(
        @PageableDefault(size = PagingDefault.EXPORT_SIZE, page = PagingDefault.PAGE)
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = sendingService.exportExcel(pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @PostMapping(value = ["import-excel"], consumes = ["multipart/form-data"])
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).I_INVENTORY.value) || hasRole('ADMIN')")
    fun importExcel(
        @RequestPart("file") file: MultipartFile
    ): ResponseEntity<BaseResponse<List<ImportSending>>> {
        val data = sendingService.importExcel(file)
        return ResponseEntity(data, HttpStatus.OK)
    }
}