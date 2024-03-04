package com.kcvn.spm.repository

import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CalculateQuantityResult
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.model.tables.references.CALCULATE_QUANTITY_RESULT
import com.kcvn.spm.model.tables.references.PRODUCT
import com.kcvn.spm.model.tables.references.WORK_RESULT
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class CalculateQuantityReportRepository(
    private val context: DSLContext,
) : SortingRepository(){
    fun findById(request: String): CalculateQuantityResult? {
        return context.selectFrom(CALCULATE_QUANTITY_RESULT)
            .where(CALCULATE_QUANTITY_RESULT.ID.eq(request).and(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false)))
            .fetchOneInto(CalculateQuantityResult::class.java)
    }

    fun update(data: CalculateQuantityResult?): CalculateQuantityResult? {
        return context.update(CALCULATE_QUANTITY_RESULT)
            .set(CALCULATE_QUANTITY_RESULT.MONTH_REPORT, data?.monthReport)
            .set(CALCULATE_QUANTITY_RESULT.START_DATE, data?.startDate)
            .set(CALCULATE_QUANTITY_RESULT.END_DATE, data?.endDate)
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
                CALCULATE_QUANTITY_RESULT.START_DATE.ge(request.startDate)
                    .and(CALCULATE_QUANTITY_RESULT.END_DATE.le(request.endDate))
                    .and(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false))
            )
            .fetchOneInto(CalculateQuantityResult::class.java)
    }

    fun getPagingListCalculateQuantityResult(pageable: Pageable): Pair<List<CalculateQuantityResult>, Int> {
        val data = context.selectFrom(CALCULATE_QUANTITY_RESULT)
            .where(CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false))
            .orderBy(getSortFields(pageable.sort, CALCULATE_QUANTITY_RESULT.MONTH_REPORT))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(CalculateQuantityResult::class.java)

        val total = context.fetchCount(CALCULATE_QUANTITY_RESULT, CALCULATE_QUANTITY_RESULT.IS_DELETED.eq(false))

        return Pair(data, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "monthreport" -> {
                CALCULATE_QUANTITY_RESULT.MONTH_REPORT
            }
            else -> {
                val errorMessage = CommonUtils.getMessage("sort.error.columnNotFound")
                throw InvalidDataAccessApiUsageException(errorMessage)
            }
        }

        return sortField
    }

}