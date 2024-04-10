package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.PlanCalendarConfig
import com.kcvn.spm.model.tables.references.PLAN_CALENDAR_CONFIG
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class PlanCalendarConfigRepository(private val context: DSLContext) {

    fun getConfigByMonth(month: Int, year: Int): PlanCalendarConfig? {
        return context.selectFrom(PLAN_CALENDAR_CONFIG)
            .where(
                PLAN_CALENDAR_CONFIG.MONTH.eq(month)
                    .and(PLAN_CALENDAR_CONFIG.YEAR.eq(year))
                    .and(PLAN_CALENDAR_CONFIG.IS_DELETED.eq(false))
            ).fetchInto(PlanCalendarConfig::class.java).firstOrNull()
    }
}