package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.ProcessGroup
import com.kcvn.spm.model.tables.references.PROCESS_GROUP
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProcessGroupRepository (private val context: DSLContext) {

    fun getForProduct() : List<ProcessGroup> {
        return context.selectFrom(PROCESS_GROUP)
            .where(PROCESS_GROUP.IS_DELETED.eq(false).and(PROCESS_GROUP.ALLOW_SHOW_PRODUCT.eq(true)))
            .fetchInto(ProcessGroup::class.java)
    }

    fun getForPlanSummary() : List<ProcessGroup> {
        return context.selectFrom(PROCESS_GROUP)
            .where(PROCESS_GROUP.IS_DELETED.eq(false).and(PROCESS_GROUP.ALLOW_SHOW_PLAN_SUMMARY.eq(true)))
            .fetchInto(ProcessGroup::class.java)
    }

    fun getForPlanEquipmentProductivity() : List<ProcessGroup> {
        return context.selectFrom(PROCESS_GROUP)
            .where(PROCESS_GROUP.IS_DELETED.eq(false).and(PROCESS_GROUP.ALLOW_SHOW_EQUIPMENT_PRODUCTIVITY.eq(true)))
            .fetchInto(ProcessGroup::class.java)
    }
}