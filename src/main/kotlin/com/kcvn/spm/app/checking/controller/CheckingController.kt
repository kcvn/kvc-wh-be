package com.kcvn.spm.app.checking.controller

import com.kcvn.spm.app.checking.payload.request.CheckingRequest
import com.kcvn.spm.app.checking.service.CheckingService
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.validation.Valid
import mu.KotlinLogging
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/checking")
class CheckingController(private val checkingService: CheckingService) {
    private val logger = LoggerFactory.getLogger(javaClass)
    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).WH.value) || hasRole('ADMIN')")
    fun createChecking(@Valid @RequestBody request: List<CheckingRequest>?): ResponseEntity<*> {
        logger.info("📥 [POST] checking/create with params: $request")
        checkingService.saveChecking(request!!)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded")),
            HttpStatus.CREATED
        )
    }
}