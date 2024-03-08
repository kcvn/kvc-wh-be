package com.kcvn.spm.app.masterdata.controller

import com.kcvn.spm.app.masterdata.payload.response.MasterDataSelectionResponse
import com.kcvn.spm.app.masterdata.service.MasterDataService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/md")
class MasterDataController (private val masterDataService: MasterDataService) {
    @GetMapping("/dropdown")
    fun getDropdownCommon(): ResponseEntity<MasterDataSelectionResponse> {
        val data = masterDataService.getMasterDataSelection()
        return ResponseEntity<MasterDataSelectionResponse>(data, HttpStatus.OK)
    }

    @GetMapping("/download-template-excel")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = masterDataService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }
}