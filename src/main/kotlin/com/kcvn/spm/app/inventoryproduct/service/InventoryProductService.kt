package com.kcvn.spm.app.inventoryproduct.service

import com.kcvn.spm.app.inventoryproduct.payload.response.CheckInventoryDateResponse
import com.kcvn.spm.repository.InventoryProductRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.OffsetDateTime


@Service
@Transactional
class InventoryProductService(
   private val inventoryProductRepository: InventoryProductRepository
) {
    fun checkInventoryDate(date: OffsetDateTime) : CheckInventoryDateResponse?{
        val data = CheckInventoryDateResponse()
        val query = inventoryProductRepository.findDateInventoryProduct(date)
        if(query != null) {
            data.hasInventoryDate = true
            return data
        }
        return data
    }
}