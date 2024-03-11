package com.kcvn.spm.repository

import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.common.constants.OrderFilterType
import com.kcvn.spm.model.tables.pojos.Plan
import com.kcvn.spm.model.tables.pojos.PlanProduct
import com.kcvn.spm.model.tables.references.PLAN
import com.kcvn.spm.model.tables.references.PLAN_PRODUCT
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class PlanRepository (private val context: DSLContext) {
    fun getListPlan(request: PlanSearchRequest, pageable: Pageable): Pair<List<PlanProduct>, Int> {
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

        when(request.filterType) {
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
        condition = condition.and(PLAN.IS_ACTIVE.eq(true)).and(PLAN_PRODUCT.IS_DELETED.eq(false))

        val query = context.select().from(PLAN_PRODUCT)
            .join(PLAN).on(PLAN_PRODUCT.PLAN_ID.eq(PLAN.ID).and(PLAN.IS_DELETED.eq(false)))
            .where(condition)

        val count = query.count()
        val data = query
            .orderBy(PLAN_PRODUCT.PRODUCT_NAME.sort(SortOrder.ASC))
            .limit(pageable.pageSize)
            .offset(pageable.offset)
            .fetchInto(PlanProduct::class.java)

        return Pair(data, count)
    }

    fun getPlanByOrderCode(orderCode: String): Plan? {
        return context.selectFrom(PLAN)
            .where(PLAN.ORDER_CODE.eq(orderCode).and(PLAN.IS_ACTIVE.eq(true)).and(PLAN.IS_DELETED.eq(false)))
            .fetchInto(Plan::class.java)
            .firstOrNull()
    }
}