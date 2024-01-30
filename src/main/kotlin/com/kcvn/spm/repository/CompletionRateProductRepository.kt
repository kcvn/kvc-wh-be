package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.CompletionRateProduct
import com.kcvn.spm.model.tables.references.COMPLETION_RATE_PRODUCT
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class CompletionRateProductRepository(private val context: DSLContext) {
    fun getByProduct(productNames: List<String>) : List<CompletionRateProduct> {
        return context.selectFrom(COMPLETION_RATE_PRODUCT)
            .where(COMPLETION_RATE_PRODUCT.PRODUCT_NAME.`in`(productNames).and(COMPLETION_RATE_PRODUCT.IS_DELETED.eq(false)))
            .fetchInto(CompletionRateProduct::class.java)
    }
}