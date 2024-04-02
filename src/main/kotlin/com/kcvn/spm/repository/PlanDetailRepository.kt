package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.PlanDetail
import com.kcvn.spm.model.tables.references.PLAN_DETAIL
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class PlanDetailRepository(private val context: DSLContext) {
    fun getPlanDetail(planProcessIds: List<String>, startDate: OffsetDateTime?, endDate: OffsetDateTime?): List<PlanDetail> {
        var condition = DSL.noCondition().and(PLAN_DETAIL.PLAN_PROCESS_ID.`in`(planProcessIds))
            .and(PLAN_DETAIL.IS_DELETED.eq(false))
        if (startDate != null && endDate != null) {
            condition = condition.and(PLAN_DETAIL.PLAN_DATE.ge(startDate)).and(PLAN_DETAIL.PLAN_DATE.le(endDate))
        }
        return context.selectFrom(PLAN_DETAIL)
            .where(condition)
            .fetchInto(PlanDetail::class.java)
    }
}