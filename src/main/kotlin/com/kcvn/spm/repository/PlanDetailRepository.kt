package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.PlanDetail
import com.kcvn.spm.model.tables.references.PLAN_DETAIL
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PlanDetailRepository(private val context: DSLContext) {
    fun getPlanDetail(planProcessIds: List<String>): List<PlanDetail> {
        return context.selectFrom(PLAN_DETAIL)
            .where(PLAN_DETAIL.PLAN_PROCESS_ID.`in`(planProcessIds).and(PLAN_DETAIL.IS_DELETED.eq(false)))
            .fetchInto(PlanDetail::class.java)
    }
}