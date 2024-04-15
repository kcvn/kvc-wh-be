package com.kcvn.spm.repository

import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.PlanProcess
import com.kcvn.spm.model.tables.pojos.PlanProcessTemp
import com.kcvn.spm.model.tables.references.PLAN_PROCESS
import com.kcvn.spm.model.tables.references.PLAN_PROCESS_TEMP
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class PlanProcessRepository(private val context: DSLContext): SortingRepository() {
    fun getListPlanProcess(planProductId: String): List<PlanProcess> {
        val sortFields = getSortFields(null, PLAN_PROCESS.PLAN_PRODUCT_ID).distinct().toMutableList()
        sortFields.add(0, DSL.cast(PLAN_PROCESS.LAYER_CODE, java.math.BigDecimal::class.java).asc())
        sortFields.add(1, PLAN_PROCESS.PROCESS_SEQUENCE.asc())

        return context.selectFrom(PLAN_PROCESS)
            .where(PLAN_PROCESS.PLAN_PRODUCT_ID.eq(planProductId).and(PLAN_PROCESS.IS_DELETED.eq(false)))
            .orderBy(sortFields).fetchInto(PlanProcess::class.java)
    }

    fun getListPlanProcess(planProductIds: List<String>): List<PlanProcess> {
        val sortFields = getSortFields(null, PLAN_PROCESS.PLAN_PRODUCT_ID).distinct().toMutableList()
        sortFields.add(0, PLAN_PROCESS.PLAN_PRODUCT_ID.asc())
        sortFields.add(1, DSL.cast(PLAN_PROCESS.LAYER_CODE, java.math.BigDecimal::class.java).asc())
        sortFields.add(2, PLAN_PROCESS.PROCESS_SEQUENCE.asc())
        return context.selectFrom(PLAN_PROCESS)
            .where(PLAN_PROCESS.PLAN_PRODUCT_ID.`in`(planProductIds).and(PLAN_PROCESS.IS_DELETED.eq(false)))
            .orderBy(sortFields).fetchInto(PlanProcess::class.java)
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