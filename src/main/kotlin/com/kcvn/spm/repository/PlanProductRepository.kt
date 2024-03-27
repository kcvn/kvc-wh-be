package com.kcvn.spm.repository

import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.model.tables.pojos.PlanProduct
import com.kcvn.spm.model.tables.references.PLAN
import com.kcvn.spm.model.tables.references.PLAN_PROCESS
import com.kcvn.spm.model.tables.references.PLAN_PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class PlanProductRepository(private val context: DSLContext) {
    fun getListPlanProduct(request: PlanSearchRequest, pageable: Pageable): Pair<List<PlanProduct>, Int> {
        val condition = searchCondition(request)
        val query = context.select(
            PLAN_PRODUCT.ID,
            PLAN_PRODUCT.PLAN_ID,
            PLAN_PRODUCT.PRODUCT_NAME,
            PLAN_PRODUCT.FRAME_1,
            PLAN_PRODUCT.MOLD,
            PLAN_PRODUCT.PCS_SH,
            PLAN_PRODUCT.BLOCK_SH
        ).from(PLAN_PRODUCT)
            .join(PLAN).on(PLAN_PRODUCT.PLAN_ID.eq(PLAN.ID).and(PLAN.IS_DELETED.eq(false)))
            .join(PLAN_PROCESS).on(PLAN_PRODUCT.ID.eq(PLAN_PROCESS.PLAN_PRODUCT_ID).and(PLAN_PROCESS.IS_DELETED.eq(false)))
            .where(condition)
            .groupBy(
                PLAN_PRODUCT.ID,
                PLAN_PRODUCT.PLAN_ID,
                PLAN_PRODUCT.PRODUCT_NAME,
                PLAN_PRODUCT.FRAME_1,
                PLAN_PRODUCT.MOLD,
                PLAN_PRODUCT.PCS_SH,
                PLAN_PRODUCT.BLOCK_SH
            )

        val count = query.count()
        val data = query
            .orderBy(PLAN_PRODUCT.PRODUCT_NAME.sort(SortOrder.ASC))
            .limit(pageable.pageSize)
            .offset(pageable.offset)
            .fetchInto(PlanProduct::class.java)

        return Pair(data, count)
    }

    fun getListPlanProduct(request: PlanSearchRequest): List<PlanProduct> {
        val condition = searchCondition(request)
        val query = context.select(
            PLAN_PRODUCT.ID,
            PLAN_PRODUCT.PLAN_ID,
            PLAN_PRODUCT.PRODUCT_NAME,
            PLAN_PRODUCT.FRAME_1,
            PLAN_PRODUCT.MOLD,
            PLAN_PRODUCT.PCS_SH,
            PLAN_PRODUCT.BLOCK_SH
        ).from(PLAN_PRODUCT)
            .join(PLAN).on(PLAN_PRODUCT.PLAN_ID.eq(PLAN.ID).and(PLAN.IS_DELETED.eq(false)))
            .join(PLAN_PROCESS).on(PLAN_PRODUCT.ID.eq(PLAN_PROCESS.PLAN_PRODUCT_ID).and(PLAN_PROCESS.IS_DELETED.eq(false)))
            .where(condition)
            .groupBy(
                PLAN_PRODUCT.ID,
                PLAN_PRODUCT.PLAN_ID,
                PLAN_PRODUCT.PRODUCT_NAME,
                PLAN_PRODUCT.FRAME_1,
                PLAN_PRODUCT.MOLD,
                PLAN_PRODUCT.PCS_SH,
                PLAN_PRODUCT.BLOCK_SH
            )

        val data = query
            .orderBy(PLAN_PRODUCT.PRODUCT_NAME.sort(SortOrder.ASC))
            .fetchInto(PlanProduct::class.java)

        return data
    }

    fun getById(id: String): PlanProduct? {
        return context.selectFrom(PLAN_PRODUCT)
            .where(PLAN_PRODUCT.ID.eq(id).and(PLAN_PRODUCT.IS_DELETED.eq(false)))
            .fetchInto(PlanProduct::class.java)
            .firstOrNull()
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
        if (!request.processGroups.isNullOrEmpty()) {
            val lstProcessGroup = request.processGroups!!.split(",").map { x -> x.trim() }
            condition = condition.and(PLAN_PROCESS.PROCESS_GROUP.`in`(lstProcessGroup))
        }
        if (request.startDate != null) {
            condition = condition.and(PLAN.START_DATE.ge(request.startDate))
        }
        if (request.endDate != null) {
            condition = condition.and(PLAN.START_DATE.le(request.endDate))
        }
        condition = condition.and(PLAN.IS_ACTIVE.eq(true)).and(PLAN_PRODUCT.IS_DELETED.eq(false))

        return condition
    }
}