package com.kcvn.spm.sample.controller

import com.kcvn.spm.sample.service.ImportExportService
import jakarta.servlet.http.HttpServletResponse
import org.springframework.batch.core.Job
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import org.springframework.web.servlet.mvc.method.annotation.StreamingResponseBody

@RestController
@RequestMapping("/api")
class ImportExportController(
    private val importExcelJob: Job,
    private val exportCsvJob: Job,
    private val importUserJob: Job,
    private val importExportService: ImportExportService
) {
    @PostMapping(value = ["/import/users/csv"], consumes = ["multipart/form-data"])
    fun importCsvUsers(@RequestPart("file") multipartFile: MultipartFile): ResponseEntity<*> {
        return importExportService.import(importUserJob, multipartFile)
    }

    @PostMapping(value = ["/import/users/excel"], consumes = ["multipart/form-data"])
    fun importExcel(@RequestPart("file") multipartFile: MultipartFile): ResponseEntity<*> {
        return importExportService.import(importExcelJob, multipartFile)
    }

    @GetMapping("/export/users/csv")
    fun exportCsv(response: HttpServletResponse): StreamingResponseBody {
        return importExportService.export(response, exportCsvJob, "exported_users.csv")

    }
}