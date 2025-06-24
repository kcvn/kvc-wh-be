package com.kcvn.spm.app.splitting.controller

import com.kcvn.spm.app.splitting.payload.request.SplittingRequest
import com.kcvn.spm.app.splitting.service.SplittingService
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
@RequestMapping("/api/splitting")
class SplittingController(private val splittingService: SplittingService) {
    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun createSplitting(@Valid @RequestBody request: List<SplittingRequest>?): ResponseEntity<*> {
        val data = splittingService.saveSplitting(request!!)
        val logger = KotlinLogging.logger {}
        logger.info(
            "USER: " + CommonUtils.loggedInUser() + ", API: post splitting/create" + ", REQUEST: " + request
        )
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded"), data),
            HttpStatus.CREATED
        )
    }
}