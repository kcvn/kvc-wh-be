package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ReceivingTransactions
import com.kcvn.spm.model.tables.references.RECEIVING_TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.springframework.stereotype.Repository

@Repository
class ReceivingTransactionsRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun findRecTrans(locationCode: String, poNumber: String, qty: Int, seqNo: Int): ReceivingTransactions? {
        return context.selectFrom(RECEIVING_TRANSACTIONS)
            .where(RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE.eq(locationCode)
                .and(RECEIVING_TRANSACTIONS.PO_NUMBER.eq(poNumber))
                .and(RECEIVING_TRANSACTIONS.QTY.eq(qty))
                .and(RECEIVING_TRANSACTIONS.SEQ_NO.eq(seqNo)))
            .fetchInto(ReceivingTransactions::class.java)
            .firstOrNull()
    }

    fun findLatestByLocationCodeAndPO(locationCode: String, poNumber: String): ReceivingTransactions? {
        return context.selectFrom(RECEIVING_TRANSACTIONS).where(RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE.eq(locationCode).and(RECEIVING_TRANSACTIONS.PO_NUMBER.eq(poNumber)))
            .orderBy(RECEIVING_TRANSACTIONS.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(ReceivingTransactions::class.java)
            .firstOrNull()
    }

    fun save(rec: ReceivingTransactions) {
        context.insertInto(RECEIVING_TRANSACTIONS, RECEIVING_TRANSACTIONS.SOURCE_LOCATION_CODE, RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE, RECEIVING_TRANSACTIONS.PO_NUMBER,
            RECEIVING_TRANSACTIONS.QTY, RECEIVING_TRANSACTIONS.SEQ_NO, RECEIVING_TRANSACTIONS.CREATED_BY)
            .values(rec.sourceLocationCode, rec.destLocationCode, rec.poNumber, rec.qty, rec.seqNo, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> RECEIVING_TRANSACTIONS.DEST_LOCATION_CODE
            "createdDate" -> RECEIVING_TRANSACTIONS.CREATED_DATE
            else -> RECEIVING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}