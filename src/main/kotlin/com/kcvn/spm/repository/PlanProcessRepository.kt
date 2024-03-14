package com.kcvn.spm.repository

import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.common.constants.OrderFilterType
import com.kcvn.spm.model.tables.pojos.PlanProcess
import com.kcvn.spm.model.tables.references.PLAN
import com.kcvn.spm.model.tables.references.PLAN_PROCESS
import com.kcvn.spm.model.tables.references.PLAN_PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PlanProcessRepository(private val context: DSLContext) {
    fun getListPlanProcess(planProductId: String): List<PlanProcess> {
        return context.selectFrom(PLAN_PROCESS)
            .where(PLAN_PROCESS.PLAN_PRODUCT_ID.eq(planProductId).and(PLAN_PROCESS.IS_DELETED.eq(false)))
            .orderBy(
                PLAN_PROCESS.LAYER_CODE.sort(SortOrder.ASC),
                PLAN_PROCESS.PROCESS_SEQUENCE.sort(SortOrder.ASC)
            ).fetchInto(PlanProcess::class.java)
    }

    fun getListPlanProcess(planProductIds: List<String>): List<PlanProcess> {
        return context.selectFrom(PLAN_PROCESS)
            .where(PLAN_PROCESS.PLAN_PRODUCT_ID.`in`(planProductIds).and(PLAN_PROCESS.IS_DELETED.eq(false)))
            .orderBy(
                PLAN_PROCESS.PLAN_PRODUCT_ID.sort(SortOrder.ASC),
                PLAN_PROCESS.LAYER_CODE.sort(SortOrder.ASC),
                PLAN_PROCESS.PROCESS_SEQUENCE.sort(SortOrder.ASC)
            ).fetchInto(PlanProcess::class.java)
    }

    fun getPlanProcessSummary(request: PlanSearchRequest): List<PlanProcess>{
        val condition = searchCondition(request)
        val data = context.select(
            PLAN_PROCESS.ID,
            PLAN_PROCESS.PLAN_ID,
            PLAN_PROCESS.PLAN_PRODUCT_ID,
            PLAN_PROCESS.PROCESS_CODE,
            PLAN_PROCESS.PROCESS_NAME,
            PLAN_PROCESS.PROCESS_CONVERT_CODE,
            PLAN_PROCESS.LAYER_CODE,
            PLAN_PROCESS.COMPLETION_RATE,
            PLAN_PROCESS.INVENTORY,
            PLAN_PROCESS.PARENT_ID,
            PLAN_PROCESS.UNIT,
            PLAN_PROCESS.PROCESS_SEQUENCE
        ).from(PLAN_PROCESS)
            .join(PLAN_PRODUCT).on(PLAN_PROCESS.PLAN_PRODUCT_ID.eq(PLAN_PRODUCT.ID).and(PLAN_PRODUCT.IS_DELETED.eq(false)))
            .join(PLAN).on(PLAN_PRODUCT.PLAN_ID.eq(PLAN.ID).and(PLAN.IS_DELETED.eq(false)))
            .where(condition)
            .orderBy(
                PLAN_PROCESS.PLAN_PRODUCT_ID.sort(SortOrder.ASC),
                PLAN_PROCESS.LAYER_CODE.sort(SortOrder.ASC),
                PLAN_PROCESS.PROCESS_SEQUENCE.sort(SortOrder.ASC)
            ).fetchInto(PlanProcess::class.java)

        return data
    }

    private fun searchCondition(request: PlanSearchRequest): Condition {
        var condition = DSL.noCondition()
        if (!request.productName.isNullOrEmpty()) {
            condition = condition.and(PLAN_PRODUCT.PRODUCT_NAME.containsIgnoreCase(request.productName?.lowercase()))
        }
        if (!request.frame_1.isNullOrEmpty()) {
            condition = condition.and(PLAN_PRODUCT.FRAME_1.eq(request.frame_1))
        }
        if (!request.mold.isNullOrEmpty()) {
            condition = condition.and(PLAN_PRODUCT.MOLD.eq(request.mold))
        }

        when (request.filterType) {
            OrderFilterType.DATE -> {
                if (request.startDate != null) {
                    condition = condition.and(PLAN.START_DATE.ge(request.startDate))
                }
                if (request.endDate != null) {
                    condition = condition.and(PLAN.END_DATE.le(request.endDate))
                }
            }

            OrderFilterType.ORDER -> {
                if (!request.orderCode.isNullOrEmpty())
                    condition = condition.and(PLAN.ORDER_CODE.eq(request.orderCode))
            }
        }
        condition = condition.and(PLAN.IS_ACTIVE.eq(true))
            .and(PLAN_PROCESS.IS_DELETED.eq(false))
            .and(PLAN_PROCESS.PARENT_ID.isNull)

        return condition
    }
}