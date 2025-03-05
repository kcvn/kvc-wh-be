package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CancelSendingTransactions
import com.kcvn.spm.model.tables.references.CANCEL_SENDING_TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.stereotype.Repository

@Repository
class CancelMovingRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun save(cst: CancelSendingTransactions): String? =
        context.insertInto(
            CANCEL_SENDING_TRANSACTIONS, CANCEL_SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE, CANCEL_SENDING_TRANSACTIONS.DEST_LOCATION_CODE,
            CANCEL_SENDING_TRANSACTIONS.PO_NUMBER, CANCEL_SENDING_TRANSACTIONS.QTY, CANCEL_SENDING_TRANSACTIONS.SEQ_NO, CANCEL_SENDING_TRANSACTIONS.RECEIVING_SEQ_NO, CANCEL_SENDING_TRANSACTIONS.CREATED_BY)
            .values(cst.sourceLocationCode, cst.destLocationCode, cst.poNumber, cst.qty, cst.seqNo, cst.receivingSeqNo, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .returningResult(CANCEL_SENDING_TRANSACTIONS.ID)
            .fetchOne()?.value1()

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "sourceLocationCode" -> CANCEL_SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE
            "createdDate" -> CANCEL_SENDING_TRANSACTIONS.CREATED_DATE
            else -> CANCEL_SENDING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}