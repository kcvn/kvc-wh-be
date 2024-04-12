package com.kcvn.spm.repository

import com.kcvn.spm.app.plan.payload.model.PlanProductCreateModel
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Plan
import com.kcvn.spm.model.tables.pojos.PlanProcessTemp
import com.kcvn.spm.model.tables.pojos.PlanProductTemp
import com.kcvn.spm.model.tables.pojos.PlanTemp
import com.kcvn.spm.model.tables.references.PLAN
import com.kcvn.spm.model.tables.references.PLAN_DETAIL_TEMP
import com.kcvn.spm.model.tables.references.PLAN_PROCESS_TEMP
import com.kcvn.spm.model.tables.references.PLAN_PRODUCT_TEMP
import com.kcvn.spm.model.tables.references.PLAN_TEMP
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PlanRepository(private val context: DSLContext) {

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
                PLAN_TEMP.CREATED_BY
            ).values(
                plan.planCode, plan.description, plan.month, plan.year,
                plan.startDate, plan.endDate, plan.isActive, plan.version, createdBy
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
                        .planProcesses.first { m -> m.processCode == x.processCode }
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
                            PLAN_DETAIL_TEMP.CREATED_BY
                        ).values(
                            x.planId, x.planProductId, x.id, m.title, m.planDate, m.sheetQuantity, m.blockQuantity, createdBy
                        )
                    }
                    query
                }.flatten()
                transactionalContext.batch(queryPlanDetail).execute()
            }
        }
    }
}