package com.kcvn.spm.repository

import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.PlanProcess
import com.kcvn.spm.model.tables.references.PLAN_PROCESS
import com.kcvn.spm.model.tables.references.PLAN_PROCESS_TEMP
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PlanProcessRepository(private val context: DSLContext) : SortingRepository() {
    fun getListPlanProcess(planProductId: String, isDraft: Boolean = false): List<PlanProcess> {
        var sortFields = getSortFields(null, PLAN_PROCESS.PLAN_PRODUCT_ID).distinct().toMutableList()
        sortFields.add(0, DSL.cast(PLAN_PROCESS.LAYER_CODE, java.math.BigDecimal::class.java).asc())
        sortFields.add(1, PLAN_PROCESS.PROCESS_SEQUENCE.asc())

        val data = context.selectFrom(PLAN_PROCESS)
            .where(PLAN_PROCESS.PLAN_PRODUCT_ID.eq(planProductId).and(PLAN_PROCESS.IS_DELETED.eq(false)))
            .orderBy(sortFields).fetchInto(PlanProcess::class.java)

        if (isDraft) {
            sortFields = getSortFields(null, PLAN_PROCESS_TEMP.PLAN_PRODUCT_ID).distinct().toMutableList()
            sortFields.add(0, DSL.cast(PLAN_PROCESS_TEMP.LAYER_CODE, java.math.BigDecimal::class.java).asc())
            sortFields.add(1, PLAN_PROCESS_TEMP.PROCESS_SEQUENCE.asc())
            data.addAll(
                context.selectFrom(PLAN_PROCESS_TEMP)
                    .where(PLAN_PROCESS_TEMP.PLAN_PRODUCT_ID.eq(planProductId).and(PLAN_PROCESS_TEMP.IS_DELETED.eq(false)))
                    .orderBy(sortFields).fetchInto(PlanProcess::class.java)
            )
        }
        return data
    }

    fun getListPlanProcess(planProductIds: List<String>, isDraft: Boolean, processGroups: String?): List<PlanProcess> {
        var sortFields = getSortFields(null, PLAN_PROCESS.PLAN_PRODUCT_ID).distinct().toMutableList()
        sortFields.add(0, PLAN_PROCESS.PLAN_PRODUCT_ID.asc())
        sortFields.add(1, DSL.cast(PLAN_PROCESS.LAYER_CODE, java.math.BigDecimal::class.java).asc())
        sortFields.add(2, PLAN_PROCESS.PROCESS_SEQUENCE.asc())

        var condition = PLAN_PROCESS.PLAN_PRODUCT_ID.`in`(planProductIds).and(PLAN_PROCESS.IS_DELETED.eq(false))
        if (!processGroups.isNullOrEmpty()) {
            val lstProcessGroup = processGroups.split(",").map { x -> x.trim() }
            condition = condition.and(PLAN_PROCESS.PROCESS_GROUP.`in`(lstProcessGroup))
        }

        val data = context.selectFrom(PLAN_PROCESS)
            .where(condition)
            .orderBy(sortFields).fetchInto(PlanProcess::class.java)

        if (isDraft) {
            sortFields = getSortFields(null, PLAN_PROCESS_TEMP.PLAN_PRODUCT_ID).distinct().toMutableList()
            sortFields.add(0, PLAN_PROCESS_TEMP.PLAN_PRODUCT_ID.asc())
            sortFields.add(1, DSL.cast(PLAN_PROCESS_TEMP.LAYER_CODE, java.math.BigDecimal::class.java).asc())
            sortFields.add(2, PLAN_PROCESS_TEMP.PROCESS_SEQUENCE.asc())

            var conditionTemp = PLAN_PROCESS_TEMP.PLAN_PRODUCT_ID.`in`(planProductIds).and(PLAN_PROCESS_TEMP.IS_DELETED.eq(false))
            if (!processGroups.isNullOrEmpty()) {
                val lstProcessGroup = processGroups.split(",").map { x -> x.trim() }
                conditionTemp = conditionTemp.and(PLAN_PROCESS_TEMP.PROCESS_GROUP.`in`(lstProcessGroup))
            }

            data.addAll(
                context.selectFrom(PLAN_PROCESS_TEMP)
                    .where(conditionTemp)
                    .orderBy(sortFields).fetchInto(PlanProcess::class.java)
            )
        }
        return data
    }

    fun getPlanProcessTemp(): List<PlanProcess> {
        return context.selectFrom(PLAN_PROCESS_TEMP)
            .where(PLAN_PROCESS_TEMP.IS_DELETED.eq(false))
            .fetchInto(PlanProcess::class.java)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "planproductid" -> PLAN_PROCESS.PLAN_PRODUCT_ID
            "layercode" -> PLAN_PROCESS.LAYER_CODE
            "processsequence" -> PLAN_PROCESS.PROCESS_SEQUENCE
            else -> PLAN_PROCESS.PLAN_PRODUCT_ID
        }
        return sortField
    }
}