package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempCheckingImportedDate
import com.kcvn.spm.model.tables.references.TEMP_CHECKING_IMPORTED_DATE
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class TempCheckingImportedDateRepository(private val context: DSLContext) : SortingRepository() {
    fun getList() : Pair<List<TempCheckingImportedDate>, Int> {
        val userName = CommonUtils.loggedInUser() ?: ""
        val query = context.selectFrom(TEMP_CHECKING_IMPORTED_DATE).where(TEMP_CHECKING_IMPORTED_DATE.CREATED_BY.eq(userName))
        val count = query.count()
        val data = query
            .orderBy(TEMP_CHECKING_IMPORTED_DATE.PO_NUMBER.asc())
            .fetchInto(TempCheckingImportedDate::class.java)

        return Pair(data, count)
    }

    fun saveAll(dataList: List<TempCheckingImportedDate>): Int {
        if (dataList.isEmpty()) return 0

        return context.transactionResult { configuration ->
            val transactionalContext = DSL.using(configuration)

            val result = transactionalContext.batchInsert(
                dataList.map { data ->
                    TEMP_CHECKING_IMPORTED_DATE.newRecord().apply {
                        this.poNumber = data.poNumber
                        this.qty = data.qty
                        this.createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM
                    }
                }
            ).execute()

            result.sum()
        }
    }

    fun delete(userName: String) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(TEMP_CHECKING_IMPORTED_DATE).where(TEMP_CHECKING_IMPORTED_DATE.CREATED_BY.eq(userName))
                .execute()
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "poNumber" -> TEMP_CHECKING_IMPORTED_DATE.PO_NUMBER
            "createdDate" -> TEMP_CHECKING_IMPORTED_DATE.CREATED_DATE
            else -> TEMP_CHECKING_IMPORTED_DATE.CREATED_DATE
        }
        return sortField
    }
}