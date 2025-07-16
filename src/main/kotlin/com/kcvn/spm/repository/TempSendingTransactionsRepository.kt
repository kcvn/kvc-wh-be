package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempSendingTransactions
import com.kcvn.spm.model.tables.references.TEMP_SENDING_TRANSACTIONS
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.stereotype.Repository

@Repository
class TempSendingTransactionsRepository(private val context: DSLContext) : SortingRepository() {
    fun saveTempSendingTrans(record: TempSendingTransactions) {
        context.insertInto(
            TEMP_SENDING_TRANSACTIONS, TEMP_SENDING_TRANSACTIONS.FORM_CODE, TEMP_SENDING_TRANSACTIONS.SOURCE_LOCATION_CODE, TEMP_SENDING_TRANSACTIONS.DEST_LOCATION_CODE,
            TEMP_SENDING_TRANSACTIONS.SOURCE_PACKAGE_CODE, TEMP_SENDING_TRANSACTIONS.DEST_PACKAGE_CODE, TEMP_SENDING_TRANSACTIONS.PO_NUMBER,
            TEMP_SENDING_TRANSACTIONS.QTY, TEMP_SENDING_TRANSACTIONS.SEQ_NO, TEMP_SENDING_TRANSACTIONS.TRANSACTION_TYPE,
            TEMP_SENDING_TRANSACTIONS.RECEIVING_DATE, TEMP_SENDING_TRANSACTIONS.INSPECTION_DATE, TEMP_SENDING_TRANSACTIONS.CREATED_BY
        )
            .values(
                record.formCode,
                record.sourceLocationCode, record.destLocationCode,
                record.sourcePackageCode, record.destPackageCode, record.poNumber,
                record.qty, record.seqNo, record.transactionType,
                record.receivingDate, record.inspectionDate, CommonUtils.loggedInUser() ?: Constants.SYSTEM
            )
            .execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> TEMP_SENDING_TRANSACTIONS.CREATED_DATE
            else -> TEMP_SENDING_TRANSACTIONS.CREATED_DATE
        }
        return sortField
    }
}