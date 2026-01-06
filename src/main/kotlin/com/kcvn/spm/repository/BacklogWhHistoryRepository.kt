package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.BacklogWhHistory
import com.kcvn.spm.model.tables.references.BACKLOG_WH
import com.kcvn.spm.model.tables.references.BACKLOG_WH_HISTORY
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal

@Repository
class BacklogWhHistoryRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun save(data: BacklogWhHistory) {
        context.insertInto(
            BACKLOG_WH_HISTORY, BACKLOG_WH_HISTORY.LOCATION_CODE, BACKLOG_WH_HISTORY.PO_NUMBER, BACKLOG_WH_HISTORY.PACKAGE_CODE,
            BACKLOG_WH_HISTORY.BACKLOG_QTY, BACKLOG_WH_HISTORY.BOX_QTY, BACKLOG_WH_HISTORY.RECEIVING_DATE,
            BACKLOG_WH_HISTORY.INSPECTION_DATE, BACKLOG_WH_HISTORY.TRANSACTION_TYPE, BACKLOG_WH_HISTORY.CREATED_BY
        )
            .values(
                data.locationCode, data.poNumber, data.packageCode,
                data.backlogQty, data.boxQty, data.receivingDate,
                data.inspectionDate, data.transactionType, CommonUtils.loggedInUser() ?: Constants.SYSTEM
            )
            .execute()
    }

    fun getBacklogHistoryList(packageCode: String) : Pair<List<BacklogWhHistory>, Int> {
        var condition: Condition = DSL.noCondition()
        condition = condition.and(BACKLOG_WH_HISTORY.PACKAGE_CODE.eq(packageCode))
        val query = context.select(
            BACKLOG_WH_HISTORY.LOCATION_CODE,
            BACKLOG_WH_HISTORY.PO_NUMBER,
            BACKLOG_WH_HISTORY.INSPECTION_DATE,
            BACKLOG_WH_HISTORY.RECEIVING_DATE,
            BACKLOG_WH_HISTORY.BACKLOG_QTY,
            BACKLOG_WH_HISTORY.BOX_QTY,
            BACKLOG_WH_HISTORY.TRANSACTION_TYPE
        )
            .from(BACKLOG_WH_HISTORY)
            .where(condition)
            .orderBy(BACKLOG_WH_HISTORY.CREATED_DATE.asc(), BACKLOG_WH_HISTORY.TRANSACTION_TYPE.desc())

        val data = query.fetch { record ->
            BacklogWhHistory(
                locationCode = record[BACKLOG_WH_HISTORY.LOCATION_CODE],
                poNumber = record[BACKLOG_WH_HISTORY.PO_NUMBER],
                receivingDate = record[BACKLOG_WH_HISTORY.RECEIVING_DATE],
                inspectionDate = record[BACKLOG_WH_HISTORY.INSPECTION_DATE],
                backlogQty = record[BACKLOG_WH_HISTORY.BACKLOG_QTY],
                boxQty = record[BACKLOG_WH_HISTORY.BOX_QTY],
                transactionType = record[BACKLOG_WH_HISTORY.TRANSACTION_TYPE],
            )
        }
        return Pair(data, data.size)
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