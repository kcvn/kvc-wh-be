package com.kcvn.spm.repository

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CheckingHistory
import com.kcvn.spm.model.tables.references.CHECKING_HISTORY
import com.kcvn.spm.model.tables.references.SENDING_TRANSACTIONS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class CheckingHistoryRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: CheckingHistorySearchRequest, pageable: Pageable) : Pair<List<CheckingHistory>, Int> {
        var condition: Condition = DSL.noCondition()
        val scanDate = CHECKING_HISTORY.field("scan_date", java.time.OffsetDateTime::class.java)
        if (!request.poNumber.isNullOrEmpty()) {
            condition = condition.and(CHECKING_HISTORY.PO_NUMBER.eq(request.poNumber))
        }
        if (!request.formCode.isNullOrEmpty()) {
            condition = condition.and(CHECKING_HISTORY.FORM_CODE.eq(request.formCode))
        }
        if (request.fromDate != null && request.toDate != null) {
            condition = condition.and(scanDate?.between(request.fromDate, request.toDate))
        }
        val query = context.selectFrom(CHECKING_HISTORY).where(condition)
        val count = query.count()
        val data = query
            .orderBy(getSortFields(pageable.sort, CHECKING_HISTORY.CREATED_DATE))
            .limit(pageable.pageSize)
            .offset(pageable.offset)
            .fetchInto(CheckingHistory::class.java)

        return Pair(data, count)
    }

    fun findByScanDateAndPOAndSeqNo(scanDate: LocalDate, poNumber: String, seqNo: Int): CheckingHistory? {
        return context.selectFrom(CHECKING_HISTORY)
            .where(
                CHECKING_HISTORY.SCAN_DATE.eq(scanDate)
                    .and(CHECKING_HISTORY.PO_NUMBER.eq(poNumber))
                    .and(CHECKING_HISTORY.SEQ_NO.eq(seqNo))
            )
            .fetchInto(CheckingHistory::class.java)
            .firstOrNull()
    }

    fun findLatestByScanDateAndPO(scanDate: LocalDate, poNumber: String): CheckingHistory? {
        return context.selectFrom(CHECKING_HISTORY)
            .where(
                CHECKING_HISTORY.SCAN_DATE.eq(scanDate)
                    .and(CHECKING_HISTORY.PO_NUMBER.eq(poNumber))
            )
            .orderBy(CHECKING_HISTORY.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(CheckingHistory::class.java)
            .firstOrNull()
    }

    fun save(data: CheckingHistory) {
        context.insertInto(
            CHECKING_HISTORY, CHECKING_HISTORY.PO_NUMBER, CHECKING_HISTORY.IMPORT_QTY,
            CHECKING_HISTORY.SCAN_QTY, CHECKING_HISTORY.SEQ_NO, CHECKING_HISTORY.FORM_CODE, SENDING_TRANSACTIONS.CREATED_BY
        )
            .values(
                data.poNumber, data.importQty,
                data.scanQty, data.seqNo, data.formCode, CommonUtils.loggedInUser() ?: Constants.SYSTEM
            )
            .execute()
    }

    fun update(scanQty: BigDecimal, scanDate: LocalDate, poNumber: String, seqNo: Int) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(CHECKING_HISTORY)
                .set(CHECKING_HISTORY.SCAN_QTY, scanQty)
                .set(CHECKING_HISTORY.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(CHECKING_HISTORY.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    CHECKING_HISTORY.SCAN_DATE.eq(scanDate)
                        .and(CHECKING_HISTORY.PO_NUMBER.eq(poNumber))
                        .and(CHECKING_HISTORY.SEQ_NO.eq(seqNo))
                )
                .execute()
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> CHECKING_HISTORY.CREATED_DATE
            else -> CHECKING_HISTORY.CREATED_DATE
        }
        return sortField
    }
}