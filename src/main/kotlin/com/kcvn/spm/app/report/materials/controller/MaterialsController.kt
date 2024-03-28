package com.kcvn.spm.app.report.materials.controller

import com.kcvn.spm.app.report.materials.payload.request.ImportTapeRequest
import com.kcvn.spm.app.report.materials.service.MaterialsService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile

@RestController
@RequestMapping("/api/report/materials-report")
class MaterialsController(
    private val materialsService: MaterialsService
) {
    @GetMapping("/download-template-excel")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = materialsService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @PostMapping(value = ["/import-excel"], consumes = ["multipart/form-data"])
    fun importCsv(request: ImportTapeRequest, @RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = materialsService.importExcelTape(request,file)
        return ResponseEntity(data, HttpStatus.OK)
    }
}