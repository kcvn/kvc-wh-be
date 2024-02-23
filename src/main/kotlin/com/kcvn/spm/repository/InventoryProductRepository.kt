package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.InventoryProduct
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.model.tables.references.INVENTORY_PRODUCT
import com.kcvn.spm.model.tables.references.PRODUCT_PROCESS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class InventoryProductRepository(private val context: DSLContext)
{
    fun findDateInventoryProduct (date: OffsetDateTime) : InventoryProduct?{
        return context.selectFrom(INVENTORY_PRODUCT)
            .where(INVENTORY_PRODUCT.INVENTORY_DATE.eq(date))
            .fetchAnyInto(InventoryProduct::class.java)
    }

    fun findInventoryProduct(id: String?) : InventoryProduct? {
        return  context.selectFrom(INVENTORY_PRODUCT)
            .where(INVENTORY_PRODUCT.PROCESS_PROCEDURE_STRUCTURE_ID.eq(id))
            .fetchAnyInto(InventoryProduct::class.java)
    }

    fun insertInventoryProduct(request: InventoryProduct)  {
        val record = context.newRecord(INVENTORY_PRODUCT, request)
        context.insertInto(INVENTORY_PRODUCT).set(record).execute()
    }

    fun updateInventoryProduct(request: InventoryProduct)  {
        val record = context.newRecord(INVENTORY_PRODUCT, request)
        context.update(INVENTORY_PRODUCT).set(record).execute()
    }
}