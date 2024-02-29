package com.kcvn.spm.repository

import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CalculateQuantityResult
import com.kcvn.spm.model.tables.references.CALCULATE_QUANTITY_RESULT
import com.kcvn.spm.model.tables.references.PRODUCT
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.jooq.impl.QOM.Extract
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.YearMonth
import java.time.ZoneOffset
import kotlin.time.Duration.Companion.days

@Repository
class CalculateQuantityReportRepository(
    private val context: DSLContext,
) {
    fun findById(request: String): CalculateQuantityResult? {
        return context.selectFrom(CALCULATE_QUANTITY_RESULT)
            .where(CALCULATE_QUANTITY_RESULT.ID.eq(request).and(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false)))
            .fetchOneInto(CalculateQuantityResult::class.java)
    }

    fun update(data: CalculateQuantityResult?): CalculateQuantityResult? {
        return context.update(CALCULATE_QUANTITY_RESULT)
            .set(CALCULATE_QUANTITY_RESULT.MONTH_REPORT, data?.monthReport)
            .set(CALCULATE_QUANTITY_RESULT.ORDER_DATE_FROM, data?.orderDateFrom)
            .set(CALCULATE_QUANTITY_RESULT.ORDER_DATE_TO, data?.orderDateTo)
            .set(CALCULATE_QUANTITY_RESULT.STATUS, true)
            .set(CALCULATE_QUANTITY_RESULT.CALCULATE_BY, data?.calculateBy)
            .set(CALCULATE_QUANTITY_RESULT.CALCULATE_DATE, data?.calculateDate)
            .set(CALCULATE_QUANTITY_RESULT.LOCKED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
            .set(CALCULATE_QUANTITY_RESULT.LOCKED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .set(PRODUCT.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
            .set(PRODUCT.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .where(CALCULATE_QUANTITY_RESULT.ID.eq(data?.id).and(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false)))
            .returningResult(CALCULATE_QUANTITY_RESULT)
            .fetchInto(CalculateQuantityResult::class.java).firstOrNull()
    }

    fun findByMonthReport(request: CalculateQuantityRequest): CalculateQuantityResult? {
        return context.selectFrom(CALCULATE_QUANTITY_RESULT)
            .where(
                CALCULATE_QUANTITY_RESULT.ORDER_DATE_FROM.ge(request.startDate)
                    .and(CALCULATE_QUANTITY_RESULT.ORDER_DATE_TO.le(request.endDate))
                    .and(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false))
            )
            .fetchOneInto(CalculateQuantityResult::class.java)
    }

}