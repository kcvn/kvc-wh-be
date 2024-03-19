package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.PlanProcess
import com.kcvn.spm.model.tables.references.PLAN_PROCESS
import org.jooq.DSLContext
import org.jooq.SortField
import org.jooq.SortOrder
import org.springframework.stereotype.Repository

@Repository
class PlanProcessRepository(private val context: DSLContext) {
    fun getListPlanProcess(planProductId: String): List<PlanProcess> {
        val sortFields = mutableListOf<SortField<*>>(
            PLAN_PROCESS.LAYER_CODE.sort(SortOrder.ASC),
            PLAN_PROCESS.PROCESS_SEQUENCE.sort(SortOrder.ASC)
        )
        return context.selectFrom(PLAN_PROCESS)
            .where(PLAN_PROCESS.PLAN_PRODUCT_ID.eq(planProductId).and(PLAN_PROCESS.IS_DELETED.eq(false)))
            .orderBy(sortFields).fetchInto(PlanProcess::class.java)
    }

    fun getListPlanProcess(planProductIds: List<String>): List<PlanProcess> {
        val sortFields = mutableListOf<SortField<*>>(
            PLAN_PROCESS.PLAN_PRODUCT_ID.sort(SortOrder.ASC),
            PLAN_PROCESS.LAYER_CODE.sort(SortOrder.ASC),
            PLAN_PROCESS.PROCESS_SEQUENCE.sort(SortOrder.ASC)
        )
        return context.selectFrom(PLAN_PROCESS)
            .where(PLAN_PROCESS.PLAN_PRODUCT_ID.`in`(planProductIds).and(PLAN_PROCESS.IS_DELETED.eq(false)))
            .orderBy(sortFields).fetchInto(PlanProcess::class.java)
    }

}