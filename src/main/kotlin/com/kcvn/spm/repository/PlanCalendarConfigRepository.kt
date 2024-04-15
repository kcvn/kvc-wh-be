package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.PlanCalendarConfig
import com.kcvn.spm.model.tables.references.PLAN_CALENDAR_CONFIG
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset

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

    fun add(data: PlanCalendarConfig) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.insertInto(
                PLAN_CALENDAR_CONFIG,
                PLAN_CALENDAR_CONFIG.MONTH,
                PLAN_CALENDAR_CONFIG.YEAR,
                PLAN_CALENDAR_CONFIG.START_DATE,
                PLAN_CALENDAR_CONFIG.END_DATE,
                PLAN_CALENDAR_CONFIG.CREATED_BY
            ).values(
                data.month,
                data.year,
                data.startDate,
                data.endDate,
                CommonUtils.loggedInUser() ?: Constants.SYSTEM
            ).execute()
        }
    }

    fun update(data: PlanCalendarConfig) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(PLAN_CALENDAR_CONFIG)
                .set(PLAN_CALENDAR_CONFIG.START_DATE, data.startDate)
                .set(PLAN_CALENDAR_CONFIG.END_DATE, data.endDate)
                .set(PLAN_CALENDAR_CONFIG.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(PLAN_CALENDAR_CONFIG.UPDATED_DATE, Instant.now().atOffset(ZoneOffset.UTC))
                .where(PLAN_CALENDAR_CONFIG.MONTH.eq(data.month).and(PLAN_CALENDAR_CONFIG.YEAR.eq(data.year)))
                .execute()
        }
    }

    fun isOverlap(data: PlanCalendarConfig): Boolean {
        var condition = DSL.noCondition()
        condition = condition.and(PLAN_CALENDAR_CONFIG.IS_DELETED.eq(false))
            .and(
                (PLAN_CALENDAR_CONFIG.END_DATE.ge(data.startDate).and(PLAN_CALENDAR_CONFIG.END_DATE.le(data.endDate)))
                    .or(PLAN_CALENDAR_CONFIG.START_DATE.le(data.endDate).and(PLAN_CALENDAR_CONFIG.END_DATE.ge(data.endDate)))
            )

        val exist = context.selectFrom(PLAN_CALENDAR_CONFIG).where(condition).fetchInto(PlanCalendarConfig::class.java).firstOrNull()
        return exist != null
    }
}