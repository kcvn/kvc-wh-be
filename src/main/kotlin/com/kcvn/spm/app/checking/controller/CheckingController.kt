package com.kcvn.spm.app.checking.controller

import com.kcvn.spm.app.checking.payload.request.CheckingRequest
import com.kcvn.spm.app.checking.service.CheckingService
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.validation.Valid
import mu.KotlinLogging
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
    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun createChecking(@Valid @RequestBody request: List<CheckingRequest>?): ResponseEntity<*> {
        checkingService.saveChecking(request!!)
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: post checking/create" + ", REQUEST: " + request
        )
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded")),
            HttpStatus.CREATED
        )
    }
}