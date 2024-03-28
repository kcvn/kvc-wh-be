package com.kcvn.spm.app.masterdata.controller

import com.kcvn.spm.app.masterdata.payload.response.MasterDataSelectionResponse
import com.kcvn.spm.app.masterdata.service.MasterDataService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/md")
class MasterDataController (private val masterDataService: MasterDataService) {
    @GetMapping("/dropdown")
    fun getDropdownCommon(): ResponseEntity<MasterDataSelectionResponse> {
        val data = masterDataService.getMasterDataSelection()
        return ResponseEntity<MasterDataSelectionResponse>(data, HttpStatus.OK)
    }


}