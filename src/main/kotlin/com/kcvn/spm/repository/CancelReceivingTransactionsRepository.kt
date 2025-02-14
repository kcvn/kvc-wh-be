package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CancelReceivingTransactions
import com.kcvn.spm.model.tables.references.CANCEL_RECEIVING_TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.stereotype.Repository

@Repository
class CancelReceivingTransactionsRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun save(rec: CancelReceivingTransactions): String? =
        context.insertInto(
            CANCEL_RECEIVING_TRANSACTIONS, CANCEL_RECEIVING_TRANSACTIONS.LOCATION_CODE, CANCEL_RECEIVING_TRANSACTIONS.PO_NUMBER,
            CANCEL_RECEIVING_TRANSACTIONS.QTY, CANCEL_RECEIVING_TRANSACTIONS.SEQ_NO, CANCEL_RECEIVING_TRANSACTIONS.CREATED_BY)
            .values(rec.locationCode, rec.poNumber, rec.qty, rec.seqNo, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .returningResult(CANCEL_RECEIVING_TRANSACTIONS.ID)
            .fetchOne()?.value1()

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> CANCEL_RECEIVING_TRANSACTIONS.LOCATION_CODE
            "createdDate" -> CANCEL_RECEIVING_TRANSACTIONS.CREATED_DATE
            else -> CANCEL_RECEIVING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}