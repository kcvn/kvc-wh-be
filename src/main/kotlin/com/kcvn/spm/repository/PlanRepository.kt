package com.kcvn.spm.repository

import com.kcvn.spm.app.plan.payload.model.PlanProductCreateModel
import com.kcvn.spm.app.plan.payload.model.PlanTempModel
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Plan
import com.kcvn.spm.model.tables.pojos.PlanDetail
import com.kcvn.spm.model.tables.pojos.PlanDetailTemp
import com.kcvn.spm.model.tables.pojos.PlanProcess
import com.kcvn.spm.model.tables.pojos.PlanProcessTemp
import com.kcvn.spm.model.tables.pojos.PlanProduct
import com.kcvn.spm.model.tables.pojos.PlanProductTemp
import com.kcvn.spm.model.tables.pojos.PlanTemp
import com.kcvn.spm.model.tables.references.PLAN
import com.kcvn.spm.model.tables.references.PLAN_DETAIL
import com.kcvn.spm.model.tables.references.PLAN_DETAIL_TEMP
import com.kcvn.spm.model.tables.references.PLAN_PROCESS
import com.kcvn.spm.model.tables.references.PLAN_PROCESS_TEMP
import com.kcvn.spm.model.tables.references.PLAN_PRODUCT
import com.kcvn.spm.model.tables.references.PLAN_PRODUCT_TEMP
import com.kcvn.spm.model.tables.references.PLAN_TEMP
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset

@Repository
class PlanRepository(private val context: DSLContext) {

    fun getPlanById(id: String): Plan? {
        return context.selectFrom(PLAN)
            .where(PLAN.ID.eq(id).and(PLAN.IS_DELETED.eq(false)))
            .fetchInto(Plan::class.java)
            .firstOrNull()
    }

    fun getListPlanByMonth(month: Int, year: Int): List<Plan> {
        return context.selectFrom(PLAN)
            .where(PLAN.IS_DELETED.eq(false))
            .and(PLAN.MONTH.eq(month))
            .and(PLAN.YEAR.eq(year))
            .fetchInto(Plan::class.java)
    }

    fun getByMonth(month: Int, year: Int): Plan? {
        return context.selectFrom(PLAN)
            .where(
                PLAN.IS_ACTIVE.eq(true).and(PLAN.IS_DELETED.eq(false))
                    .and(PLAN.MONTH.eq(month)).and(PLAN.YEAR.eq(year))
            ).fetchInto(Plan::class.java).firstOrNull()
    }

    fun createPlanTemp(plan: PlanTemp, planProducts: List<PlanProductCreateModel>) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM

            transactionalContext.deleteFrom(PLAN_DETAIL_TEMP).execute()
            transactionalContext.deleteFrom(PLAN_PROCESS_TEMP).execute()
            transactionalContext.deleteFrom(PLAN_PRODUCT_TEMP).execute()
            transactionalContext.deleteFrom(PLAN_TEMP).execute()

            val planRecord = transactionalContext.insertInto(
                PLAN_TEMP,
                PLAN_TEMP.PLAN_CODE,
                PLAN_TEMP.DESCRIPTION,
                PLAN_TEMP.MONTH,
                PLAN_TEMP.YEAR,
                PLAN_TEMP.START_DATE,
                PLAN_TEMP.END_DATE,
                PLAN_TEMP.IS_ACTIVE,
                PLAN_TEMP.VERSION,
                PLAN_TEMP.HAS_INVENTORY,
                PLAN_TEMP.CREATED_BY
            ).values(
                plan.planCode, plan.description, plan.month, plan.year,
                plan.startDate, plan.endDate, plan.isActive, plan.version, plan.hasInventory, createdBy
            ).returningResult(PLAN_TEMP).fetchInto(PlanTemp::class.java).firstOrNull()

            if (planRecord != null) {
                val queryPlanProduct = planProducts.map { x ->
                    transactionalContext.insertInto(
                        PLAN_PRODUCT_TEMP,
                        PLAN_PRODUCT_TEMP.PLAN_ID,
                        PLAN_PRODUCT_TEMP.PRODUCT_NAME,
                        PLAN_PRODUCT_TEMP.FRAME_1,
                        PLAN_PRODUCT_TEMP.MOLD,
                        PLAN_PRODUCT_TEMP.PCS_SH,
                        PLAN_PRODUCT_TEMP.BLOCK_SH,
                        PLAN_PRODUCT_TEMP.CREATED_BY
                    ).values(
                        planRecord.id, x.productName, x.frame_1, x.mold, x.pcsSh, x.blockSh, createdBy
                    )
                }
                transactionalContext.batch(queryPlanProduct).execute()

                val planProductRecords = transactionalContext.selectFrom(PLAN_PRODUCT_TEMP)
                    .where(PLAN_PRODUCT_TEMP.PLAN_ID.eq(planRecord.id).and(PLAN_PRODUCT_TEMP.IS_DELETED.eq(false)))
                    .fetchInto(PlanProductTemp::class.java)

                val queryPlanProcess = planProductRecords.map { x ->
                    val planProduct = planProducts.first { m -> m.productName == x.productName }
                    val query = planProduct.planProcesses.map { m ->
                        transactionalContext.insertInto(
                            PLAN_PROCESS_TEMP,
                            PLAN_PROCESS_TEMP.PLAN_ID,
                            PLAN_PROCESS_TEMP.PLAN_PRODUCT_ID,
                            PLAN_PROCESS_TEMP.PROCESS_CODE,
                            PLAN_PROCESS_TEMP.PROCESS_NAME,
                            PLAN_PROCESS_TEMP.PROCESS_CONVERT_CODE,
                            PLAN_PROCESS_TEMP.LAYER_CODE,
                            PLAN_PROCESS_TEMP.COMPLETION_RATE,
                            PLAN_PROCESS_TEMP.INVENTORY,
                            PLAN_PROCESS_TEMP.UNIT,
                            PLAN_PROCESS_TEMP.PROCESS_SEQUENCE,
                            PLAN_PROCESS_TEMP.PROCESS_NAME_JP,
                            PLAN_PROCESS_TEMP.PROCESS_GROUP,
                            PLAN_PROCESS_TEMP.PROCESS_STATISTIC_CODE,
                            PLAN_PROCESS_TEMP.CREATED_BY
                        ).values(
                            x.planId, x.id, m.processCode, m.processName, m.processConvertCode,
                            m.layerCode, m.completionRate, m.inventory, m.unit, m.processSequence, m.processNameJp,
                            m.processGroup, m.processStatisticCode, createdBy
                        )
                    }
                    query
                }.flatten()
                transactionalContext.batch(queryPlanProcess).execute()

                val planProcessRecords = transactionalContext.selectFrom(PLAN_PROCESS_TEMP)
                    .where(PLAN_PROCESS_TEMP.PLAN_ID.eq(planRecord.id).and(PLAN_PROCESS_TEMP.IS_DELETED.eq(false)))
                    .fetchInto(PlanProcessTemp::class.java)

                val queryPlanChildrenProcess = planProcessRecords.map { x ->
                    val planProduct = planProductRecords.first { m -> m.id == x.planProductId }
                    val planProcess = planProducts.first { m -> m.productName == planProduct.productName }
                        .planProcesses.first { m -> m.processCode == x.processCode }
                    val query = planProcess.childrenProcesses.map { m ->
                        transactionalContext.insertInto(
                            PLAN_PROCESS_TEMP,
                            PLAN_PROCESS_TEMP.PLAN_ID,
                            PLAN_PROCESS_TEMP.PLAN_PRODUCT_ID,
                            PLAN_PROCESS_TEMP.PROCESS_CODE,
                            PLAN_PROCESS_TEMP.PROCESS_NAME,
                            PLAN_PROCESS_TEMP.PROCESS_CONVERT_CODE,
                            PLAN_PROCESS_TEMP.LAYER_CODE,
                            PLAN_PROCESS_TEMP.COMPLETION_RATE,
                            PLAN_PROCESS_TEMP.INVENTORY,
                            PLAN_PROCESS_TEMP.UNIT,
                            PLAN_PROCESS_TEMP.PROCESS_SEQUENCE,
                            PLAN_PROCESS_TEMP.PROCESS_NAME_JP,
                            PLAN_PROCESS_TEMP.PROCESS_GROUP,
                            PLAN_PROCESS_TEMP.PROCESS_STATISTIC_CODE,
                            PLAN_PROCESS_TEMP.PARENT_ID,
                            PLAN_PROCESS_TEMP.CREATED_BY
                        ).values(
                            x.planId, planProduct.id, m.processCode, m.processName, m.processConvertCode,
                            m.layerCode, m.completionRate, m.inventory, m.unit, m.processSequence, m.processNameJp,
                            m.processGroup, m.processStatisticCode, x.id, createdBy
                        )
                    }
                    query
                }.flatten()
                transactionalContext.batch(queryPlanChildrenProcess).execute()

                val queryPlanDetail = planProcessRecords.map { x ->
                    val planProduct = planProductRecords.first { m -> m.id == x.planProductId }
                    val planProcess = planProducts.first { m -> m.productName == planProduct.productName }
                        .planProcesses.first { m -> m.processCode == x.processCode && m.layerCode?.toIntOrNull() == x.layerCode?.toIntOrNull() }
                    val query = planProcess.planDetails.map { m ->
                        transactionalContext.insertInto(
                            PLAN_DETAIL_TEMP,
                            PLAN_DETAIL_TEMP.PLAN_ID,
                            PLAN_DETAIL_TEMP.PLAN_PRODUCT_ID,
                            PLAN_DETAIL_TEMP.PLAN_PROCESS_ID,
                            PLAN_DETAIL_TEMP.TITLE,
                            PLAN_DETAIL_TEMP.PLAN_DATE,
                            PLAN_DETAIL_TEMP.SHEET_QUANTITY,
                            PLAN_DETAIL_TEMP.BLOCK_QUANTITY,
                            PLAN_DETAIL_TEMP.ORDER_DATE,
                            PLAN_DETAIL_TEMP.HAS_INVENTORY,
                            PLAN_DETAIL_TEMP.CREATED_BY
                        ).values(
                            x.planId, x.planProductId, x.id, m.title, m.planDate, m.sheetQuantity, m.blockQuantity,
                            m.orderDate, m.hasInventory ?: false, createdBy
                        )
                    }
                    query
                }.flatten()
                transactionalContext.batch(queryPlanDetail).execute()
            }
        }
    }

    fun getPlanTemp(): Plan? {
        return context.selectFrom(PLAN_TEMP)
            .where(PLAN_TEMP.IS_DELETED.eq(false))
            .fetchInto(Plan::class.java)
            .firstOrNull()
    }

    fun createPlan(plan: Plan, planProducts: List<PlanProduct>, planProcesses: List<PlanProcess>, planDetails: List<PlanDetail>) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM

            transactionalContext.insertInto(
                PLAN,
                PLAN.ID,
                PLAN.PLAN_CODE,
                PLAN.DESCRIPTION,
                PLAN.MONTH,
                PLAN.YEAR,
                PLAN.START_DATE,
                PLAN.END_DATE,
                PLAN.IS_ACTIVE,
                PLAN.VERSION,
                PLAN.HAS_INVENTORY,
                PLAN.CREATED_BY
            ).values(
                plan.id, plan.planCode, plan.description, plan.month, plan.year,
                plan.startDate, plan.endDate, plan.isActive, plan.version, plan.hasInventory, createdBy
            ).execute()

            val queryPlanProduct = planProducts.map { item ->
                transactionalContext.insertInto(
                    PLAN_PRODUCT,
                    PLAN_PRODUCT.ID,
                    PLAN_PRODUCT.PLAN_ID,
                    PLAN_PRODUCT.PRODUCT_NAME,
                    PLAN_PRODUCT.FRAME_1,
                    PLAN_PRODUCT.MOLD,
                    PLAN_PRODUCT.PCS_SH,
                    PLAN_PRODUCT.BLOCK_SH,
                    PLAN_PRODUCT.CREATED_BY
                ).values(
                    item.id, item.planId, item.productName, item.frame_1, item.mold, item.pcsSh, item.blockSh, createdBy
                )
            }
            transactionalContext.batch(queryPlanProduct).execute()

            val queryPlanProcess = planProcesses.map { item ->
                transactionalContext.insertInto(
                    PLAN_PROCESS,
                    PLAN_PROCESS.ID,
                    PLAN_PROCESS.PLAN_ID,
                    PLAN_PROCESS.PLAN_PRODUCT_ID,
                    PLAN_PROCESS.PROCESS_CODE,
                    PLAN_PROCESS.PROCESS_NAME,
                    PLAN_PROCESS.PROCESS_CONVERT_CODE,
                    PLAN_PROCESS.LAYER_CODE,
                    PLAN_PROCESS.COMPLETION_RATE,
                    PLAN_PROCESS.INVENTORY,
                    PLAN_PROCESS.UNIT,
                    PLAN_PROCESS.PROCESS_SEQUENCE,
                    PLAN_PROCESS.PROCESS_NAME_JP,
                    PLAN_PROCESS.PROCESS_GROUP,
                    PLAN_PROCESS.PROCESS_STATISTIC_CODE,
                    PLAN_PROCESS.PARENT_ID,
                    PLAN_PROCESS.CREATED_BY
                ).values(
                    item.id, item.planId, item.planProductId, item.processCode, item.processName, item.processConvertCode,
                    item.layerCode, item.completionRate, item.inventory, item.unit, item.processSequence, item.processNameJp,
                    item.processGroup, item.processStatisticCode, item.parentId, createdBy
                )
            }
            transactionalContext.batch(queryPlanProcess).execute()

            val queryPlanDetail = planDetails.map { item ->
                transactionalContext.insertInto(
                    PLAN_DETAIL,
                    PLAN_DETAIL.ID,
                    PLAN_DETAIL.PLAN_ID,
                    PLAN_DETAIL.PLAN_PRODUCT_ID,
                    PLAN_DETAIL.PLAN_PROCESS_ID,
                    PLAN_DETAIL.TITLE,
                    PLAN_DETAIL.PLAN_DATE,
                    PLAN_DETAIL.SHEET_QUANTITY,
                    PLAN_DETAIL.BLOCK_QUANTITY,
                    PLAN_DETAIL.ORDER_DATE,
                    PLAN_DETAIL.HAS_INVENTORY,
                    PLAN_DETAIL.CREATED_BY
                ).values(
                    item.id, item.planId, item.planProductId, item.planProcessId, item.title, item.planDate,
                    item.sheetQuantity, item.blockQuantity, item.orderDate, item.hasInventory ?: false, createdBy
                )
            }
            transactionalContext.batch(queryPlanDetail).execute()

            transactionalContext.deleteFrom(PLAN_DETAIL_TEMP).execute()
            transactionalContext.deleteFrom(PLAN_PROCESS_TEMP).execute()
            transactionalContext.deleteFrom(PLAN_PRODUCT_TEMP).execute()
            transactionalContext.deleteFrom(PLAN_TEMP).execute()
        }
    }

    fun inActive(id: String) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val updateBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM

            transactionalContext.update(PLAN)
                .set(PLAN.IS_ACTIVE, false)
                .set(PLAN.UPDATED_DATE, Instant.now().atOffset(ZoneOffset.UTC))
                .set(PLAN.UPDATED_BY, updateBy)
                .where(PLAN.ID.eq(id))
                .execute()
        }
    }

    fun getDataForRePlan(month: Int, year: Int): PlanTempModel? {
        val response = PlanTempModel()
        response.plan = context.selectFrom(PLAN_TEMP)
            .where(PLAN_TEMP.YEAR.eq(year).and(PLAN_TEMP.MONTH.eq(month)).and(PLAN_TEMP.IS_DELETED.eq(false)))
            .fetchInto(PlanTemp::class.java)
            .firstOrNull()
        if (response.plan != null) {
            response.planProducts = context.selectFrom(PLAN_PRODUCT_TEMP)
                .where(PLAN_PRODUCT_TEMP.IS_DELETED.eq(false))
                .fetchInto(PlanProductTemp::class.java)

            response.planProcesses = context.selectFrom(PLAN_PROCESS_TEMP)
                .where(PLAN_PROCESS_TEMP.IS_DELETED.eq(false))
                .fetchInto(PlanProcessTemp::class.java)

            response.planDetails = context.selectFrom(PLAN_DETAIL_TEMP)
                .where(PLAN_DETAIL_TEMP.IS_DELETED.eq(false))
                .fetchInto(PlanDetailTemp::class.java)
        } else {
            response.plan = context.selectFrom(PLAN)
                .where(PLAN.YEAR.eq(year).and(PLAN.MONTH.eq(month)).and(PLAN.IS_DELETED.eq(false)).and(PLAN.IS_ACTIVE.eq(true)))
                .fetchInto(PlanTemp::class.java)
                .firstOrNull()

            response.planProducts = context.selectFrom(PLAN_PRODUCT)
                .where(PLAN_PRODUCT.IS_DELETED.eq(false))
                .fetchInto(PlanProductTemp::class.java)

            response.planProcesses = context.selectFrom(PLAN_PROCESS)
                .where(PLAN_PROCESS.IS_DELETED.eq(false))
                .fetchInto(PlanProcessTemp::class.java)

            response.planDetails = context.selectFrom(PLAN_DETAIL)
                .where(PLAN_DETAIL.IS_DELETED.eq(false))
                .fetchInto(PlanDetailTemp::class.java)
        }
        return response
    }
}