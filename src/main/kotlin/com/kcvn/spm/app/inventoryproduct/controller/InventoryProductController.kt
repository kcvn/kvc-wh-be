package com.kcvn.spm.app.inventoryproduct.controller

import com.kcvn.spm.app.inventoryproduct.payload.response.CheckInventoryDateResponse
import com.kcvn.spm.app.inventoryproduct.service.InventoryProductService
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.util.CommonUtils
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.time.OffsetDateTime

@RestController
@RequestMapping("/api/inventory-product")
class InventoryProductController(
    private val inventoryProductService: InventoryProductService
) {
    @GetMapping("/check-inventory-date")
    fun checkInventoryDate(date: OffsetDateTime
    ) : ResponseEntity<BaseResponse<CheckInventoryDateResponse>>{
        val data = inventoryProductService.checkInventoryDate(date)
        if(data!= null && data.hasInventoryDate){
            return ResponseEntity(
                BaseResponse(data = data, message = CommonUtils.getMessage("check.inventoryDateProduct",arrayOf(data.toString()))),
                HttpStatus.OK
            )
        }else{
            return ResponseEntity(
                BaseResponse(data = data, message = "Ok"),
                HttpStatus.OK
            )
        }
    }
}