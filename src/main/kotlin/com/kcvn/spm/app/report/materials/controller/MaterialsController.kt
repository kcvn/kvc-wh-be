package com.kcvn.spm.app.report.materials.controller

import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.app.report.materials.payload.request.GetReportMaterialsRequest
import com.kcvn.spm.app.report.materials.payload.request.ImportTapeRequest
import com.kcvn.spm.app.report.materials.service.MaterialsService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.model.tables.pojos.TapeInfo
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
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

    @GetMapping("/all")
    fun getMaterials (
        request: GetReportMaterialsRequest,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["yearReport"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["monthReport"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["productName"], direction = Sort.Direction.DESC)
        )
        pageable: Pageable
    ) : ResponseEntity<BasePagingResponse<TapeInfo?>>{
        val data = materialsService.getReportMaterials(request,pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }
}