package com.kcvn.spm.app.sync.controller

import com.kcvn.spm.app.sync.service.SyncTransAmDataService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


@RestController
@RequestMapping("/api/sync")
class SyncTransAmDataController (private val syncTransAmDataService: SyncTransAmDataService) {
    @PostMapping("/process-procedure-structure")
    fun syncProcessProcedureStructure(): ResponseEntity<MessageResponse> {
        syncTransAmDataService.syncProcessProcedureStructure()
        return ResponseEntity<MessageResponse>(MessageResponse(CommonUtils.getMessage("sync.finished")), HttpStatus.OK)
    }

    @PostMapping("/process-master")
    fun syncProcessMaster(): ResponseEntity<MessageResponse> {
        syncTransAmDataService.syncProcessMaster()
        return ResponseEntity<MessageResponse>(MessageResponse(CommonUtils.getMessage("sync.finished")), HttpStatus.OK)
    }
}