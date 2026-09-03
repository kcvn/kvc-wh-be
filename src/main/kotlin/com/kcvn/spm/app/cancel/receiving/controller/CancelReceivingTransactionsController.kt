package com.kcvn.spm.app.cancel.receiving.controller

import com.kcvn.spm.app.cancel.receiving.payload.request.CancelRecTransRequest
import com.kcvn.spm.app.cancel.receiving.service.CancelReceivingTransactionsService
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
@RequestMapping("/api/cancel/receiving")
class CancelReceivingTransactionsController(
    private val cancelReceivingService: CancelReceivingTransactionsService
) {
    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun createCancelReceiving(@Valid @RequestBody request: CancelRecTransRequest?): ResponseEntity<*> {
        val cancelRec = cancelReceivingService.createCancelReceiving(request!!)
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