package com.kcvn.spm.app.masterData.controller

import com.kcvn.spm.app.masterData.payload.response.MasterDataSelectionResponse
import com.kcvn.spm.app.masterData.service.MasterDataService
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
        return try {
            val data = masterDataService.getMasterDataSelection()
            ResponseEntity<MasterDataSelectionResponse>(data, HttpStatus.OK)
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<MasterDataSelectionResponse>(null, HttpStatus.OK)
        }
    }
}