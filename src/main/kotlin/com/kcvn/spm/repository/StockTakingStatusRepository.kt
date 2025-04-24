package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.StockTakingStatus
import com.kcvn.spm.model.tables.references.STOCK_TAKING_STATUS
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class StockTakingStatusRepository(private val context: DSLContext) : SortingRepository() {
    fun findByYearAndMonth(yearNumber: Int, monthNumber: Int): StockTakingStatus? {
        return context.selectFrom(STOCK_TAKING_STATUS)
            .where(STOCK_TAKING_STATUS.YEAR_NUMBER.eq(yearNumber).and(STOCK_TAKING_STATUS.MONTH_NUMBER.eq(monthNumber)))
            .fetchInto(StockTakingStatus::class.java)
            .firstOrNull()
    }

    fun findByStatus(status: String): StockTakingStatus? {
        return context.selectFrom(STOCK_TAKING_STATUS)
            .where(STOCK_TAKING_STATUS.STATUS.eq(status))
            .fetchInto(StockTakingStatus::class.java)
            .firstOrNull()
    }

    fun save(domain: StockTakingStatus) {
        context.insertInto(
            STOCK_TAKING_STATUS, STOCK_TAKING_STATUS.YEAR_NUMBER, STOCK_TAKING_STATUS.MONTH_NUMBER,
            STOCK_TAKING_STATUS.STATUS, STOCK_TAKING_STATUS.CREATED_BY)
            .values(domain.yearNumber, domain.monthNumber, domain.status, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()
    }

    fun updateStatus(yearNumber: Int, monthNumber: Int) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(STOCK_TAKING_STATUS)
                .set(STOCK_TAKING_STATUS.STATUS, "completed")
                .set(STOCK_TAKING_STATUS.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(STOCK_TAKING_STATUS.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    STOCK_TAKING_STATUS.YEAR_NUMBER.eq(yearNumber)
                        .and(STOCK_TAKING_STATUS.MONTH_NUMBER.eq(monthNumber))
                )
                .execute()
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "yearNumber" -> STOCK_TAKING_STATUS.YEAR_NUMBER
            "monthNumber" -> STOCK_TAKING_STATUS.MONTH_NUMBER
            else -> STOCK_TAKING_STATUS.YEAR_NUMBER
        }
        return sortField
    }
}