package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWhHistory
import com.kcvn.spm.model.tables.references.BACKLOG_WH_HISTORY
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.stereotype.Repository

@Repository
class BacklogWhHistoryRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun save(data: BacklogWhHistory) {
        context.insertInto(
            BACKLOG_WH_HISTORY, BACKLOG_WH_HISTORY.LOCATION_CODE, BACKLOG_WH_HISTORY.PO_NUMBER, BACKLOG_WH_HISTORY.PACKAGE_CODE,
            BACKLOG_WH_HISTORY.BACKLOG_QTY, BACKLOG_WH_HISTORY.BOX_QTY, BACKLOG_WH_HISTORY.RECEIVING_DATE, BACKLOG_WH_HISTORY.TRANSACTION_TYPE, BACKLOG_WH_HISTORY.CREATED_BY
        )
            .values(data.locationCode, data.poNumber, data.packageCode, data.backlogQty, data.boxQty, data.receivingDate, data.transactionType, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> BACKLOG_WH_HISTORY.LOCATION_CODE
            "createdDate" -> BACKLOG_WH_HISTORY.CREATED_DATE
            else -> BACKLOG_WH_HISTORY.CREATED_DATE
        }
        return sortField
    }
}