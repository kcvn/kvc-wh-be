package com.kcvn.spm.repository

import com.kcvn.spm.app.plan.payload.request.PlanDetailRequest
import com.kcvn.spm.app.plan.payload.request.PlanHistorySearchRequest
import com.kcvn.spm.app.plan.payload.request.PlanSearchRequest
import com.kcvn.spm.model.tables.pojos.PlanProduct
import com.kcvn.spm.model.tables.pojos.PlanTemp
import com.kcvn.spm.model.tables.references.PLAN
import com.kcvn.spm.model.tables.references.PLAN_PROCESS
import com.kcvn.spm.model.tables.references.PLAN_PROCESS_TEMP
import com.kcvn.spm.model.tables.references.PLAN_PRODUCT
import com.kcvn.spm.model.tables.references.PLAN_PRODUCT_TEMP
import com.kcvn.spm.model.tables.references.PLAN_TEMP
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class PlanProductRepository(private val context: DSLContext) {

    fun getListPlanHistory(request: PlanHistorySearchRequest, pageable: Pageable): Pair<List<PlanProduct>, Int> {
        var condition = DSL.noCondition()
        condition = condition.and(PLAN_PRODUCT.IS_DELETED.eq(false))
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

        if (!request.planId.isNullOrEmpty()) {
            condition = condition.and(PLAN.ID.eq(request.planId))
        }
        val query = context.select(

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
                PLAN_PRODUCT.PRODUCT_NAME,
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



    fun getListPlanProduct(request: PlanSearchRequest, pageable: Pageable): Pair<List<PlanProduct>, Int> {
        val condition = searchCondition(request)
        if (request.draftWorkPlan == true) {
            val conditionTemp = searchConditionTemp(request)
            val planTemp = context.selectFrom(PLAN_TEMP).where(PLAN_TEMP.IS_DELETED.eq(false)).fetchInto(PlanTemp::class.java).firstOrNull()

            val queryPlan = context.select(
                PLAN_PRODUCT.PRODUCT_NAME.`as`("productName"),
                PLAN_PRODUCT.FRAME_1.`as`("frame_1"),
                PLAN_PRODUCT.MOLD.`as`("mold"),
                PLAN_PRODUCT.PCS_SH.`as`("pcsSh"),
                PLAN_PRODUCT.BLOCK_SH.`as`("blockSh")
            ).from(PLAN_PRODUCT)
                .join(PLAN).on(PLAN_PRODUCT.PLAN_ID.eq(PLAN.ID).and(PLAN.IS_DELETED.eq(false)))
                .join(PLAN_PROCESS).on(PLAN_PRODUCT.ID.eq(PLAN_PROCESS.PLAN_PRODUCT_ID).and(PLAN_PROCESS.IS_DELETED.eq(false)))
                .where(condition.and(PLAN.MONTH.notEqual(planTemp?.month ?: 0)).and(PLAN.YEAR.notEqual(planTemp?.year ?: 0)))
                .orderBy(PLAN_PRODUCT.PRODUCT_NAME.sort(SortOrder.ASC))

            val queryPlanTemp = context.select(
                PLAN_PRODUCT_TEMP.PRODUCT_NAME.`as`("productName"),
                PLAN_PRODUCT_TEMP.FRAME_1.`as`("frame_1"),
                PLAN_PRODUCT_TEMP.MOLD.`as`("mold"),
                PLAN_PRODUCT_TEMP.PCS_SH.`as`("pcsSh"),
                PLAN_PRODUCT_TEMP.BLOCK_SH.`as`("blockSh")
            ).from(PLAN_PRODUCT_TEMP)
                .join(PLAN_TEMP).on(PLAN_PRODUCT_TEMP.PLAN_ID.eq(PLAN_TEMP.ID).and(PLAN_TEMP.IS_DELETED.eq(false)))
                .join(PLAN_PROCESS_TEMP).on(PLAN_PRODUCT_TEMP.ID.eq(PLAN_PROCESS_TEMP.PLAN_PRODUCT_ID).and(PLAN_PROCESS_TEMP.IS_DELETED.eq(false)))
                .where(conditionTemp)
                .orderBy(PLAN_PRODUCT_TEMP.PRODUCT_NAME.sort(SortOrder.ASC))

            val query = context.select().from(queryPlan).union(queryPlanTemp)
            val count = query.count()
            val data = query.limit(pageable.pageSize).offset(pageable.offset).fetchInto(PlanProduct::class.java)

            return Pair(data, count)
        } else {
            val query = context.select(
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
    }

    fun getListPlanProduct(request: PlanSearchRequest): List<PlanProduct> {
        val condition = searchCondition(request)
        if (request.draftWorkPlan == true) {
            val conditionTemp = searchConditionTemp(request)
            val planTemp = context.selectFrom(PLAN_TEMP).where(PLAN_TEMP.IS_DELETED.eq(false)).fetchInto(PlanTemp::class.java).firstOrNull()

            val queryPlan = context.select(
                PLAN_PRODUCT.ID.`as`("id"),
                PLAN_PRODUCT.PRODUCT_NAME.`as`("productName"),
                PLAN_PRODUCT.FRAME_1.`as`("frame_1"),
                PLAN_PRODUCT.MOLD.`as`("mold"),
                PLAN_PRODUCT.PCS_SH.`as`("pcsSh"),
                PLAN_PRODUCT.BLOCK_SH.`as`("blockSh")
            ).from(PLAN_PRODUCT)
                .join(PLAN).on(PLAN_PRODUCT.PLAN_ID.eq(PLAN.ID).and(PLAN.IS_DELETED.eq(false)))
                .join(PLAN_PROCESS).on(PLAN_PRODUCT.ID.eq(PLAN_PROCESS.PLAN_PRODUCT_ID).and(PLAN_PROCESS.IS_DELETED.eq(false)))
                .where(condition.and(PLAN.MONTH.notEqual(planTemp?.month ?: 0)).and(PLAN.YEAR.notEqual(planTemp?.year ?: 0)))
                .orderBy(PLAN_PRODUCT.PRODUCT_NAME.sort(SortOrder.ASC))

            val queryPlanTemp = context.select(
                PLAN_PRODUCT_TEMP.ID.`as`("id"),
                PLAN_PRODUCT_TEMP.PRODUCT_NAME.`as`("productName"),
                PLAN_PRODUCT_TEMP.FRAME_1.`as`("frame_1"),
                PLAN_PRODUCT_TEMP.MOLD.`as`("mold"),
                PLAN_PRODUCT_TEMP.PCS_SH.`as`("pcsSh"),
                PLAN_PRODUCT_TEMP.BLOCK_SH.`as`("blockSh")
            ).from(PLAN_PRODUCT_TEMP)
                .join(PLAN_TEMP).on(PLAN_PRODUCT_TEMP.PLAN_ID.eq(PLAN_TEMP.ID).and(PLAN_TEMP.IS_DELETED.eq(false)))
                .join(PLAN_PROCESS_TEMP).on(PLAN_PRODUCT_TEMP.ID.eq(PLAN_PROCESS_TEMP.PLAN_PRODUCT_ID).and(PLAN_PROCESS_TEMP.IS_DELETED.eq(false)))
                .where(conditionTemp)
                .orderBy(PLAN_PRODUCT_TEMP.PRODUCT_NAME.sort(SortOrder.ASC))

            val query = context.select().from(queryPlan).union(queryPlanTemp)
            val data = query.fetchInto(PlanProduct::class.java)

            return data
        } else {
            val query = context.select(
                PLAN_PRODUCT.ID,
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
    }

    fun getById(id: String): PlanProduct? {
        return context.selectFrom(PLAN_PRODUCT)
            .where(PLAN_PRODUCT.ID.eq(id).and(PLAN_PRODUCT.IS_DELETED.eq(false)))
            .fetchInto(PlanProduct::class.java)
            .firstOrNull()
    }

    fun getForPlan(request: PlanDetailRequest): List<PlanProduct> {
        val condition = getForPlanCondition(request)
        if (request.draftWorkPlan == true) {
            val conditionTemp = getForPlanConditionTemp(request)
            val planTemp = context.selectFrom(PLAN_TEMP).where(PLAN_TEMP.IS_DELETED.eq(false)).fetchInto(PlanTemp::class.java).firstOrNull()

            val queryPlan = context.select(
                PLAN_PRODUCT.ID.`as`("id"),
                PLAN_PRODUCT.PRODUCT_NAME.`as`("productName"),
                PLAN_PRODUCT.FRAME_1.`as`("frame_1"),
                PLAN_PRODUCT.MOLD.`as`("mold"),
                PLAN_PRODUCT.PCS_SH.`as`("pcsSh"),
                PLAN_PRODUCT.BLOCK_SH.`as`("blockSh")
            ).from(PLAN_PRODUCT)
                .join(PLAN).on(PLAN_PRODUCT.PLAN_ID.eq(PLAN.ID).and(PLAN.IS_DELETED.eq(false)))
                .join(PLAN_PROCESS).on(PLAN_PRODUCT.ID.eq(PLAN_PROCESS.PLAN_PRODUCT_ID).and(PLAN_PROCESS.IS_DELETED.eq(false)))
                .where(condition.and(PLAN.MONTH.notEqual(planTemp?.month ?: 0)).and(PLAN.YEAR.notEqual(planTemp?.year ?: 0)))
                .orderBy(PLAN_PRODUCT.PRODUCT_NAME.sort(SortOrder.ASC))

            val queryPlanTemp = context.select(
                PLAN_PRODUCT_TEMP.ID.`as`("id"),
                PLAN_PRODUCT_TEMP.PRODUCT_NAME.`as`("productName"),
                PLAN_PRODUCT_TEMP.FRAME_1.`as`("frame_1"),
                PLAN_PRODUCT_TEMP.MOLD.`as`("mold"),
                PLAN_PRODUCT_TEMP.PCS_SH.`as`("pcsSh"),
                PLAN_PRODUCT_TEMP.BLOCK_SH.`as`("blockSh")
            ).from(PLAN_PRODUCT_TEMP)
                .join(PLAN_TEMP).on(PLAN_PRODUCT_TEMP.PLAN_ID.eq(PLAN_TEMP.ID).and(PLAN_TEMP.IS_DELETED.eq(false)))
                .join(PLAN_PROCESS_TEMP).on(PLAN_PRODUCT_TEMP.ID.eq(PLAN_PROCESS_TEMP.PLAN_PRODUCT_ID).and(PLAN_PROCESS_TEMP.IS_DELETED.eq(false)))
                .where(conditionTemp)
                .orderBy(PLAN_PRODUCT_TEMP.PRODUCT_NAME.sort(SortOrder.ASC))

            val query = context.select().from(queryPlan).union(queryPlanTemp)
            val data = query.fetchInto(PlanProduct::class.java)

            return data
        } else {
            val query = context.select(
                PLAN_PRODUCT.ID,
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
    }

    fun getPlanProductTemp(): List<PlanProduct> {
        return context.selectFrom(PLAN_PRODUCT_TEMP)
            .where(PLAN_PRODUCT_TEMP.IS_DELETED.eq(false))
            .fetchInto(PlanProduct::class.java)
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
        if (request.startDate != null && request.endDate != null) {
            condition = condition.and(
                (PLAN.START_DATE.ge(request.startDate).and(PLAN.START_DATE.le(request.endDate)))
                    .or(PLAN.START_DATE.le(request.startDate).and(PLAN.END_DATE.ge(request.startDate)))
            )
        }
        if (request.inventoryWorkPlan == true) {
            condition = condition.and(PLAN.HAS_INVENTORY.eq(true))
        }

        condition = condition.and(PLAN.IS_ACTIVE.eq(true)).and(PLAN_PRODUCT.IS_DELETED.eq(false))

        return condition
    }

    private fun searchConditionTemp(request: PlanSearchRequest): Condition {
        var condition = DSL.noCondition()
        if (!request.productName.isNullOrEmpty()) {
            condition = condition.and(PLAN_PRODUCT_TEMP.PRODUCT_NAME.containsIgnoreCase(request.productName?.lowercase()))
        }
        if (!request.frame_1.isNullOrEmpty()) {
            condition = condition.and(PLAN_PRODUCT_TEMP.FRAME_1.eq(request.frame_1))
        }
        if (!request.mold.isNullOrEmpty()) {
            condition = condition.and(PLAN_PRODUCT_TEMP.MOLD.eq(request.mold))
        }
        if (!request.processGroups.isNullOrEmpty()) {
            val lstProcessGroup = request.processGroups!!.split(",").map { x -> x.trim() }
            condition = condition.and(PLAN_PROCESS_TEMP.PROCESS_GROUP.`in`(lstProcessGroup))
        }
        if (request.startDate != null && request.endDate != null) {
            condition = condition.and(
                (PLAN_TEMP.START_DATE.ge(request.startDate).and(PLAN_TEMP.START_DATE.le(request.endDate)))
                    .or(PLAN_TEMP.START_DATE.le(request.startDate).and(PLAN_TEMP.END_DATE.ge(request.startDate)))
            )
        }
        if (request.inventoryWorkPlan == true) {
            condition = condition.and(PLAN_TEMP.HAS_INVENTORY.eq(true))
        }

        condition = condition.and(PLAN_TEMP.IS_ACTIVE.eq(true)).and(PLAN_PRODUCT_TEMP.IS_DELETED.eq(false))

        return condition
    }

    private fun getForPlanCondition(request: PlanDetailRequest): Condition {
        var condition = PLAN_PRODUCT.IS_DELETED.eq(false)
            .and(PLAN.IS_ACTIVE.eq(true))
            .and(PLAN_PRODUCT.PRODUCT_NAME.eq(request.productName))

        if (request.startDate != null && request.endDate != null) {
            condition = condition.and(
                (PLAN.START_DATE.ge(request.startDate).and(PLAN.START_DATE.le(request.endDate)))
                    .or(PLAN.START_DATE.le(request.startDate).and(PLAN.END_DATE.ge(request.startDate)))
            )
        }
        if (request.inventoryWorkPlan == true) {
            condition = condition.and(PLAN.HAS_INVENTORY.eq(true))
        }

        return condition
    }

    private fun getForPlanConditionTemp(request: PlanDetailRequest): Condition {
        var condition = PLAN_PRODUCT_TEMP.IS_DELETED.eq(false)
            .and(PLAN_TEMP.IS_ACTIVE.eq(true))
            .and(PLAN_PRODUCT_TEMP.PRODUCT_NAME.eq(request.productName))

        if (request.startDate != null && request.endDate != null) {
            condition = condition.and(
                (PLAN_TEMP.START_DATE.ge(request.startDate).and(PLAN_TEMP.START_DATE.le(request.endDate)))
                    .or(PLAN_TEMP.START_DATE.le(request.startDate).and(PLAN_TEMP.END_DATE.ge(request.startDate)))
            )
        }
        if (request.inventoryWorkPlan == true) {
            condition = condition.and(PLAN_TEMP.HAS_INVENTORY.eq(true))
        }

        return condition
    }
}