package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.PlanColorConfig
import com.kcvn.spm.model.tables.references.PLAN_COLOR_CONFIG
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PlanColorConfigRepository (private val context: DSLContext) {

    fun getAll(): List<PlanColorConfig> {
        return context.selectFrom(PLAN_COLOR_CONFIG).where(PLAN_COLOR_CONFIG.IS_DELETED.eq(false))
            .fetchInto(PlanColorConfig::class.java)
    }
}