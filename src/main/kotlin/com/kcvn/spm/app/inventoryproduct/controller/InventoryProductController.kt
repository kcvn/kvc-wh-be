package com.kcvn.spm.app.inventoryproduct.controller

import com.kcvn.spm.app.inventoryproduct.payload.response.CheckInventoryDateResponse
import com.kcvn.spm.app.inventoryproduct.service.InventoryProductService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.model.FileContentModel
import com.kcvn.spm.common.util.CommonUtils
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
}