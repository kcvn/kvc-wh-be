package com.kcvn.spm.app.report.materials.controller

import com.kcvn.spm.app.inventoryproduct.payload.request.InventoryProductRequest
import com.kcvn.spm.app.inventoryproduct.payload.response.CheckInventoryDateResponse
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.app.report.materials.payload.request.GetReportMaterialsRequest
import com.kcvn.spm.app.report.materials.payload.request.ImportTapeRequest
import com.kcvn.spm.app.report.materials.payload.response.CheckImportTapeResponse
import com.kcvn.spm.app.report.materials.service.MaterialsService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TapeInfo
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.Month
import java.time.OffsetDateTime
import java.time.Year

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

    @GetMapping("/check-import")
    fun checkInventoryDate(monthReport: Int, yearReport: Int
    ) : ResponseEntity<BaseResponse<CheckImportTapeResponse>>{
        val data = materialsService.checkImportTape(monthReport, yearReport)
        return if( data.hasImportTape){
            ResponseEntity(
                BaseResponse(data = data, message = CommonUtils.getMessage("validate.checkImportTape",arrayOf(monthReport, yearReport))),
                HttpStatus.OK
            )
        }else{
            ResponseEntity(
                BaseResponse(data = data, message = "Ok"),
                HttpStatus.OK
            )
        }
    }

    @GetMapping("/export-excel")
    fun exportExcel(
        request: GetReportMaterialsRequest,
        @PageableDefault(size = PagingDefault.EXPORT_SIZE, page = PagingDefault.PAGE)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["yearReport"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["monthReport"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["productName"], direction = Sort.Direction.DESC),
        )
        pageable: Pageable
    ): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = materialsService.exportExcel(request,pageable)
        return ResponseEntity(data, HttpStatus.OK)
    }
}