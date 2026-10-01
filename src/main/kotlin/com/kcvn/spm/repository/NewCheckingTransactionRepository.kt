package com.kcvn.spm.repository

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistoryRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.NewCheckingTransaction
import com.kcvn.spm.model.tables.references.CHECKING_HISTORY
import com.kcvn.spm.model.tables.references.NEW_CHECKING_TRANSACTION
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class NewCheckingTransactionRepository(private val context: DSLContext) : SortingRepository() {
        fun findByLotAndSeq(input: CheckingHistoryRequest, seqNo: Int): NewCheckingTransaction? {
        return context.selectFrom(NEW_CHECKING_TRANSACTION)
            .where(
                NEW_CHECKING_TRANSACTION.INVOICE_NUMBER.eq(input.invoiceNumber)
                    .and(NEW_CHECKING_TRANSACTION.PO_NUMBER.eq(input.poNumber))
                    .and(NEW_CHECKING_TRANSACTION.LOT_NO.eq(input.lotNo))
                    .and(NEW_CHECKING_TRANSACTION.CHECK_TIMES.eq(seqNo))
            )
            .fetchInto(NewCheckingTransaction::class.java)
            .firstOrNull()
    }

    fun findLatestSeqOfLot(input: CheckingHistoryRequest): NewCheckingTransaction? {
        return context.selectFrom(NEW_CHECKING_TRANSACTION)
            .where(
                NEW_CHECKING_TRANSACTION.INVOICE_NUMBER.eq(input.invoiceNumber)
                    .and(NEW_CHECKING_TRANSACTION.PO_NUMBER.eq(input.poNumber))
                    .and(NEW_CHECKING_TRANSACTION.LOT_NO.eq(input.lotNo))
            )
            .orderBy(NEW_CHECKING_TRANSACTION.CHECK_TIMES.sort(SortOrder.DESC))
            .fetchInto(NewCheckingTransaction::class.java)
            .firstOrNull()
    }

    fun save(data: NewCheckingTransaction) {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.newRecord(NEW_CHECKING_TRANSACTION, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }.insert()
    }

    fun update(scanQty: BigDecimal, scanDate: LocalDate, poNumber: String, seqNo: Int, formCode: String) {
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
                        .and(CHECKING_HISTORY.FORM_CODE.eq(formCode))
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