package com.kcvn.spm.repository

import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CalculateQuantityResult
import com.kcvn.spm.model.tables.references.CALCULATE_QUANTITY_RESULT
import com.kcvn.spm.model.tables.references.PRODUCT
import org.jooq.DSLContext
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class CalculateQuantityReportRepository(
    private val context: DSLContext,
) {
    fun findById(request: String): CalculateQuantityResult? {
        return context.selectFrom(CALCULATE_QUANTITY_RESULT)
         .where(CALCULATE_QUANTITY_RESULT.ID.eq(request))
         .fetchOneInto(CalculateQuantityResult::class.java)
    }

    fun update(data: CalculateQuantityResult?) : CalculateQuantityResult? {
        return context.update(CALCULATE_QUANTITY_RESULT)
            .set(CALCULATE_QUANTITY_RESULT.MONTH_REPORT,data?.monthReport)
            .set(CALCULATE_QUANTITY_RESULT.ORDER_DATE_FROM,data?.orderDateFrom)
            .set(CALCULATE_QUANTITY_RESULT.ORDER_DATE_TO,data?.orderDateTo)
            .set(CALCULATE_QUANTITY_RESULT.STATUS,true)
            .set(CALCULATE_QUANTITY_RESULT.CALCULATE_BY,data?.calculateBy)
            .set(CALCULATE_QUANTITY_RESULT.CALCULATE_DATE,data?.calculateDate)
            .set(CALCULATE_QUANTITY_RESULT.LOCKED_BY,CommonUtils.loggedInUser() ?: "SYSTEM")
            .set(CALCULATE_QUANTITY_RESULT.LOCKED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .set(PRODUCT.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
            .set(PRODUCT.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .where(CALCULATE_QUANTITY_RESULT.ID.eq(data?.id))
            .returningResult(CALCULATE_QUANTITY_RESULT)
            .fetchInto(CalculateQuantityResult::class.java).firstOrNull()
    }

}