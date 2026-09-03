package com.kcvn.spm.app.cancel.sending.controller

import com.kcvn.spm.app.cancel.sending.payload.request.CancelSendTransRequest
import com.kcvn.spm.app.cancel.sending.service.CancelSendingTransactionsService
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/cancel/sending")
class CancelSendingTransactionsController(
    private val cancelSendingService: CancelSendingTransactionsService
) {
    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun createCancelSending(@Valid @RequestBody request: CancelSendTransRequest?): ResponseEntity<*> {
        val cancelRec = cancelSendingService.createCancelSending(request!!)
        return if (cancelRec == null) {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.failed")),
                HttpStatus.BAD_REQUEST
            )
        } else {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.succeeded"), cancelRec),
                HttpStatus.CREATED
            )
        }
    }
}