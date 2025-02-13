package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ReceivingTransactions
import com.kcvn.spm.model.tables.references.RECEIVING_TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class ReceivingTransactionsRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun updateIsCanceled(data: ReceivingTransactions) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(RECEIVING_TRANSACTIONS)
                .set(RECEIVING_TRANSACTIONS.IS_CANCELED, true)
                .set(RECEIVING_TRANSACTIONS.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(RECEIVING_TRANSACTIONS.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(RECEIVING_TRANSACTIONS.LOCATION_CODE.eq(data.locationCode)
                    .and(RECEIVING_TRANSACTIONS.PO_NUMBER.eq(data.poNumber))
                    .and(RECEIVING_TRANSACTIONS.SEQ_NO.eq(data.seqNo))
                    .and(RECEIVING_TRANSACTIONS.IS_CANCELED.eq(false))
                )
                .execute()
        }
    }

    fun findLatestByLocationCodeAndPO(locationCode: String, poNumber: String): ReceivingTransactions? {
        return context.selectFrom(RECEIVING_TRANSACTIONS).where(RECEIVING_TRANSACTIONS.LOCATION_CODE.eq(locationCode).and(RECEIVING_TRANSACTIONS.PO_NUMBER.eq(poNumber)))
            .orderBy(RECEIVING_TRANSACTIONS.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(ReceivingTransactions::class.java)
            .firstOrNull()
    }

    fun save(rec: ReceivingTransactions) {
        context.insertInto(RECEIVING_TRANSACTIONS, RECEIVING_TRANSACTIONS.LOCATION_CODE, RECEIVING_TRANSACTIONS.PO_NUMBER,
            RECEIVING_TRANSACTIONS.QTY, RECEIVING_TRANSACTIONS.SEQ_NO, RECEIVING_TRANSACTIONS.CREATED_BY)
            .values(rec.locationCode, rec.poNumber, rec.qty, rec.seqNo, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> RECEIVING_TRANSACTIONS.LOCATION_CODE
            "createdDate" -> RECEIVING_TRANSACTIONS.CREATED_DATE
            else -> RECEIVING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}