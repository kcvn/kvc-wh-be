package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogBinEntry
import com.kcvn.spm.model.tables.references.BACKLOG_BIN_ENTRY
import com.kcvn.spm.model.tables.references.BACKLOG_WH
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate

@Repository
class BacklogBinEntryRepository(private val context: DSLContext) : SortingRepository() {
    fun getBinEntryFromBacklog() : Pair<List<BacklogBinEntry>, Int> {
        var condition: Condition = DSL.noCondition()
        condition = condition.and(BACKLOG_WH.BACKLOG_QTY.gt(BigDecimal.ZERO))
        val query = context.select(
            DSL.min(BACKLOG_WH.LOCATION_CODE.cast(SQLDataType.INTEGER)).`as`("MIN_LOCATION_CODE"),
            BACKLOG_WH.PO_NUMBER,
            DSL.sum(BACKLOG_WH.BACKLOG_QTY).`as`("SUM_BACKLOG_QTY"),
            BACKLOG_WH.RECEIVING_DATE,
            DSL.max(BACKLOG_WH.INSPECTION_DATE).`as`("INSPECTION_DATE")
        )
            .from(BACKLOG_WH)
            .where(condition)
            .groupBy(BACKLOG_WH.PO_NUMBER, BACKLOG_WH.RECEIVING_DATE)
            .orderBy(BACKLOG_WH.RECEIVING_DATE.sort(SortOrder.ASC))

        val data = query.fetch { record ->
            BacklogBinEntry(
                locationCode = record.get("MIN_LOCATION_CODE", BigDecimal::class.java).toString(),
                poNumber = record[BACKLOG_WH.PO_NUMBER],
                backlogQty = record.get("SUM_BACKLOG_QTY", BigDecimal::class.java) ?: BigDecimal.ZERO,
                receivingDate = record[BACKLOG_WH.RECEIVING_DATE],
                inspectionDate = record.get("INSPECTION_DATE", LocalDate::class.java)
            )
        }
        return Pair(data, data.size)
    }

    fun saveAll(dataList: List<BacklogBinEntry>): Int {
        if (dataList.isEmpty()) return 0

        return context.batchInsert(
                dataList.map { data ->
                    BACKLOG_BIN_ENTRY.newRecord().apply {
                        this.locationCode = data.locationCode
                        this.poNumber = data.poNumber
                        this.backlogQty = data.backlogQty
                        this.receivingDate = data.receivingDate
                        this.inspectionDate = data.inspectionDate
                        this.createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM
                    }
                }
            ).execute().sum()
    }

    fun delete() {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(BACKLOG_BIN_ENTRY)
                .execute()
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> BACKLOG_BIN_ENTRY.LOCATION_CODE
            else -> BACKLOG_BIN_ENTRY.LOCATION_CODE
        }
        return sortField
    }
}