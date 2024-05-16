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
            val queryDetail = context.select(
                PLAN_DETAIL.PLAN_ID.`as`("planId"),
                PLAN_DETAIL.PLAN_PRODUCT_ID.`as`("planProductId"),
                PLAN_DETAIL.PLAN_PROCESS_ID.`as`("planProcessId"),
                PLAN_DETAIL.TITLE.`as`("title"),
                PLAN_DETAIL.PLAN_DATE.`as`("planDate"),
                PLAN_DETAIL.SHEET_QUANTITY.`as`("sheetQuantity"),
                PLAN_DETAIL.BLOCK_QUANTITY.`as`("blockQuantity"),
                PLAN_DETAIL.ORDER_DATE.`as`("orderDate"),
                PLAN_DETAIL.HAS_INVENTORY.`as`("hasInventory")
            ).from(PLAN_DETAIL).where(condition)

            val queryDetailTemp = context.select(
                PLAN_DETAIL_TEMP.PLAN_ID.`as`("planId"),
                PLAN_DETAIL_TEMP.PLAN_PRODUCT_ID.`as`("planProductId"),
                PLAN_DETAIL_TEMP.PLAN_PROCESS_ID.`as`("planProcessId"),
                PLAN_DETAIL_TEMP.TITLE.`as`("title"),
                PLAN_DETAIL_TEMP.PLAN_DATE.`as`("planDate"),
                PLAN_DETAIL_TEMP.SHEET_QUANTITY.`as`("sheetQuantity"),
                PLAN_DETAIL_TEMP.BLOCK_QUANTITY.`as`("blockQuantity"),
                PLAN_DETAIL_TEMP.ORDER_DATE.`as`("orderDate"),
                PLAN_DETAIL_TEMP.HAS_INVENTORY.`as`("hasInventory")
            ).from(PLAN_DETAIL_TEMP).where(conditionTemp)

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