package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.PlanDetail
import com.kcvn.spm.model.tables.references.PLAN_DETAIL
import com.kcvn.spm.model.tables.references.PLAN_DETAIL_TEMP
import org.jooq.Condition
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class PlanDetailRepository(private val context: DSLContext) {
    fun getPlanDetail(
        planProcessIds: List<String>,
        startDate: OffsetDateTime?,
        endDate: OffsetDateTime?,
        hasInventory: Boolean? = null,
        isDraft: Boolean? = null
    ): List<PlanDetail> {
        val condition = getCondition(planProcessIds, startDate, endDate, hasInventory)
        if (isDraft == true) {
            val conditionTemp = getConditionTemp(planProcessIds, startDate, endDate, hasInventory)
            val queryDetail = context.selectFrom(PLAN_DETAIL).where(condition)
            val queryDetailTemp = context.selectFrom(PLAN_DETAIL_TEMP).where(conditionTemp)

            return context.select().from(queryDetail).unionAll(queryDetailTemp)
                .fetchInto(PlanDetail::class.java)
        } else {
            return context.selectFrom(PLAN_DETAIL)
                .where(condition)
                .fetchInto(PlanDetail::class.java)
        }
    }

    fun getPlanDetailTemp(): List<PlanDetail> {
        return context.selectFrom(PLAN_DETAIL_TEMP)
            .where(PLAN_DETAIL_TEMP.IS_DELETED.eq(false))
            .fetchInto(PlanDetail::class.java)
    }

    private fun getCondition(planProcessIds: List<String>, startDate: OffsetDateTime?, endDate: OffsetDateTime?, hasInventory: Boolean? = null): Condition {
        var condition = PLAN_DETAIL.PLAN_PROCESS_ID.`in`(planProcessIds)
            .and(PLAN_DETAIL.IS_DELETED.eq(false))
        if (startDate != null && endDate != null) {
            condition = condition.and(PLAN_DETAIL.PLAN_DATE.ge(startDate)).and(PLAN_DETAIL.PLAN_DATE.le(endDate))
        }
        if (hasInventory == true) {
            condition = condition.and(PLAN_DETAIL.HAS_INVENTORY.eq(true))
        }
        return condition
    }

    private fun getConditionTemp(planProcessIds: List<String>, startDate: OffsetDateTime?, endDate: OffsetDateTime?, hasInventory: Boolean? = null): Condition {
        var condition = PLAN_DETAIL_TEMP.PLAN_PROCESS_ID.`in`(planProcessIds)
            .and(PLAN_DETAIL_TEMP.IS_DELETED.eq(false))
        if (startDate != null && endDate != null) {
            condition = condition.and(PLAN_DETAIL_TEMP.PLAN_DATE.ge(startDate)).and(PLAN_DETAIL_TEMP.PLAN_DATE.le(endDate))
        }
        if (hasInventory == true) {
            condition = condition.and(PLAN_DETAIL_TEMP.HAS_INVENTORY.eq(true))
        }
        return condition
    }
}