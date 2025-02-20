package com.kcvn.spm.app.transaction.receiving.controller

import com.kcvn.spm.app.transaction.receiving.payload.RecTransRequest
import com.kcvn.spm.app.transaction.receiving.service.ReceivingTransactionsService
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
@RequestMapping("/api/receiving")
class ReceivingTransactionsController(private val receivingService: ReceivingTransactionsService) {
    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun createRecTrans(@Valid @RequestBody request: List<RecTransRequest>?): ResponseEntity<*> {
        receivingService.saveRecTrans(request!!)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded")),
            HttpStatus.CREATED
        )
    }
}