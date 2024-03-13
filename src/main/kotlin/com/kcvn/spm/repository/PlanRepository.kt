package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.Plan
import com.kcvn.spm.model.tables.references.PLAN
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PlanRepository(private val context: DSLContext) {
    fun getPlanByOrderCode(orderCode: String): Plan? {
        return context.selectFrom(PLAN)
            .where(PLAN.ORDER_CODE.eq(orderCode).and(PLAN.IS_ACTIVE.eq(true)).and(PLAN.IS_DELETED.eq(false)))
            .fetchInto(Plan::class.java)
            .firstOrNull()
    }
}