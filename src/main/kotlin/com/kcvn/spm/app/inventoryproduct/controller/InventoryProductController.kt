package com.kcvn.spm.app.inventoryproduct.controller

import com.kcvn.spm.app.inventoryproduct.payload.request.InventoryProductRequest
import com.kcvn.spm.app.inventoryproduct.payload.response.CheckInventoryDateResponse
import com.kcvn.spm.app.inventoryproduct.payload.response.InventoryProductResponse
import com.kcvn.spm.app.inventoryproduct.service.InventoryProductService
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.InventoryProduct
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*
import org.springframework.web.multipart.MultipartFile
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter

@RestController
@RequestMapping("/api/inventory-product")
class InventoryProductController(
    private val inventoryProductService: InventoryProductService
) {
    @GetMapping("/check-inventory-date")
    fun checkInventoryDate(date: OffsetDateTime
    ) : ResponseEntity<BaseResponse<CheckInventoryDateResponse>>{
        val data = inventoryProductService.checkInventoryDate(date)
        val localDate = date.toLocalDate() // Chuyển đổi OffsetDateTime thành LocalDate
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy") // Định dạng của chuỗi
        val formattedDate = localDate.format(formatter) // Định dạng lại LocalDate thành chuỗi
        if(data!= null && data.hasInventoryDate){
            return ResponseEntity(
                BaseResponse(data = data, message = CommonUtils.getMessage("check.inventoryDateProduct",arrayOf(formattedDate.toString()))),
                HttpStatus.OK
            )
        }else{
            return ResponseEntity(
                BaseResponse(data = data, message = "Ok"),
                HttpStatus.OK
            )
        }
    }

    @GetMapping("/download-template-excel")
    fun downloadTemplateExcel(): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = inventoryProductService.downloadTemplate()
        return ResponseEntity(data, HttpStatus.OK)
    }

    @PostMapping(value = ["/import-excel"], consumes = ["multipart/form-data"])
    fun importCsv(@RequestPart("file") file: MultipartFile): ResponseEntity<BaseResponse<FileContentModel>> {
        val data = inventoryProductService.importExelInventoryProduct(file)
        return ResponseEntity(data, HttpStatus.OK)
    }

    @GetMapping("/all")
    fun  getAllInventoryProduct (
        request: InventoryProductRequest,
        @PageableDefault(size = 10, page = 0)
        @SortDefault.SortDefaults(
            SortDefault(sort = ["inventoryDate"], direction = Sort.Direction.DESC),
            SortDefault(sort = ["productName"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["layerCode"], direction = Sort.Direction.ASC),
            SortDefault(sort = ["processName"], direction = Sort.Direction.ASC)
        )
        pageable: Pageable?
    ) :  ResponseEntity<BasePagingResponse<InventoryProductResponse>> {
        val result = inventoryProductService.getListInventoryProduct(request,pageable!!)
        return if (result.data.isNullOrEmpty()) {
            ResponseEntity(BasePagingResponse(), HttpStatus.NO_CONTENT)
        } else {
            ResponseEntity(result, HttpStatus.OK)
        }
    }
}