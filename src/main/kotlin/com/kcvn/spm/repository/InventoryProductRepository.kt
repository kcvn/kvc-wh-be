package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.InventoryProduct
import com.kcvn.spm.model.tables.references.INVENTORY_PRODUCT
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
}