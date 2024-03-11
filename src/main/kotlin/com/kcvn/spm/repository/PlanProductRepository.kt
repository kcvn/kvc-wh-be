package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.PlanProduct
import com.kcvn.spm.model.tables.references.PLAN_PRODUCT
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PlanProductRepository(private val context: DSLContext) {
    fun getById(id: String): PlanProduct? {
        return context.selectFrom(PLAN_PRODUCT)
            .where(PLAN_PRODUCT.ID.eq(id).and(PLAN_PRODUCT.IS_DELETED.eq(false)))
            .fetchInto(PlanProduct::class.java)
            .firstOrNull()
    }
}