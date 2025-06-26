package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempCheckingImported
import com.kcvn.spm.model.tables.references.TEMP_CHECKING_IMPORTED
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.format.DateTimeFormatter

@Repository
class TempCheckingImportedRepository(private val context: DSLContext) : SortingRepository() {
    fun getList() : Pair<List<TempCheckingImported>, Int> {
//        val userName = CommonUtils.loggedInUser() ?: ""
//        val query = context.selectFrom(TEMP_CHECKING_IMPORTED).where(TEMP_CHECKING_IMPORTED.CREATED_BY.eq(userName))
        val query = context.selectFrom(TEMP_CHECKING_IMPORTED)
        val count = query.count()
        val data = query
            .orderBy(TEMP_CHECKING_IMPORTED.CREATED_DATE.desc())
            .fetchInto(TempCheckingImported::class.java)

        return Pair(data, count)
    }

    fun findLatestImport(): TempCheckingImported? {
        val todayPrefix = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd")) + "%"
        return context.selectFrom(TEMP_CHECKING_IMPORTED)
            .where(TEMP_CHECKING_IMPORTED.FORM_CODE.like(todayPrefix))
            .orderBy(TEMP_CHECKING_IMPORTED.FORM_CODE.desc())
            .fetchInto(TempCheckingImported::class.java)
            .firstOrNull()
    }

    fun saveAll(dataList: List<TempCheckingImported>): Int {
        if (dataList.isEmpty()) return 0

        return context.transactionResult { configuration ->
            val transactionalContext = DSL.using(configuration)

            val result = transactionalContext.batchInsert(
                dataList.map { data ->
                    TEMP_CHECKING_IMPORTED.newRecord().apply {
                        this.poNumber = data.poNumber
                        this.qty = data.qty
                        this.formCode = data.formCode
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
            transactionalContext.deleteFrom(TEMP_CHECKING_IMPORTED).where(TEMP_CHECKING_IMPORTED.CREATED_BY.eq(userName))
                .execute()
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "poNumber" -> TEMP_CHECKING_IMPORTED.PO_NUMBER
            "createdDate" -> TEMP_CHECKING_IMPORTED.CREATED_DATE
            else -> TEMP_CHECKING_IMPORTED.CREATED_DATE
        }
        return sortField
    }
}