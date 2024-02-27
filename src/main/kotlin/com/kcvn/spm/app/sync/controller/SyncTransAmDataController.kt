package com.kcvn.spm.app.sync.controller

import com.kcvn.spm.app.sync.service.SyncTransAmDataService
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/sync")
class SyncTransAmDataController (
    private val syncTransAmDataService: SyncTransAmDataService
) {
    val finishedMessage = "sync.finished"
    @PostMapping("/process-procedure-structure")
    fun syncProcessProcedureStructure(): ResponseEntity<MessageResponse> {
        syncTransAmDataService.syncProcessProcedureStructure()
        return ResponseEntity<MessageResponse>(MessageResponse(CommonUtils.getMessage(finishedMessage)), HttpStatus.OK)
    }

    @PostMapping("/process-master")
    fun syncProcessMaster(): ResponseEntity<MessageResponse> {
        syncTransAmDataService.syncProcessMaster()
        return ResponseEntity<MessageResponse>(MessageResponse(CommonUtils.getMessage(finishedMessage)), HttpStatus.OK)
    }

    @PostMapping("/work-result")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).SY_WORK_RESULT.value) || hasRole('ADMIN')")
    fun syncWorkResult(): ResponseEntity<MessageResponse> {
        syncTransAmDataService.syncWorkResult()
        return ResponseEntity<MessageResponse>(MessageResponse(CommonUtils.getMessage(finishedMessage)), HttpStatus.OK)
    }
}