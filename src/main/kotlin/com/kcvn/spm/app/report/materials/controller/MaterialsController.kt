package com.kcvn.spm.app.report.materials.controller

import com.kcvn.spm.app.report.materials.service.MaterialsService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/materials-report")
class MaterialsController(
    private val materialsService: MaterialsService
) {
    @GetMapping("/download-template-excel")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = materialsService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

}