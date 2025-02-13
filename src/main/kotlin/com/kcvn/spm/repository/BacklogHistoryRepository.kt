package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogHistory
import com.kcvn.spm.model.tables.references.BACKLOG_HISTORY
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.stereotype.Repository

@Repository
class BacklogHistoryRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun save(rec: BacklogHistory) {
        context.insertInto(
            BACKLOG_HISTORY, BACKLOG_HISTORY.LOCATION_CODE, BACKLOG_HISTORY.PO_NUMBER,
            BACKLOG_HISTORY.BACKLOG_QTY, BACKLOG_HISTORY.BOX_QTY, BACKLOG_HISTORY.TRANSACTION_TYPE, BACKLOG_HISTORY.CREATED_BY)
            .values(rec.locationCode, rec.poNumber, rec.backlogQty, rec.boxQty, rec.transactionType, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> BACKLOG_HISTORY.LOCATION_CODE
            "createdDate" -> BACKLOG_HISTORY.CREATED_DATE
            else -> BACKLOG_HISTORY.CREATED_DATE
        }
        return sortField
    }
}