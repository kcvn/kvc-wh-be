package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempCheckingImported
import com.kcvn.spm.model.tables.pojos.TempSendingImported
import com.kcvn.spm.model.tables.references.TEMP_CHECKING_IMPORTED
import com.kcvn.spm.model.tables.references.TEMP_SENDING_IMPORTED
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalTime
import java.time.OffsetDateTime

@Repository
class TempSendingImportedRepository(private val context: DSLContext) : SortingRepository() {
    fun findByFormCode(formCode: String): TempSendingImported? {
        return context.selectFrom(TEMP_SENDING_IMPORTED)
            .where(
                TEMP_SENDING_IMPORTED.FORM_CODE.eq(formCode)
            )
            .fetchInto(TempSendingImported::class.java)
            .firstOrNull()
    }

    fun getList() : Pair<List<TempSendingImported>, Int> {
        val userName = CommonUtils.loggedInUser() ?: ""
        val query = context.selectFrom(TEMP_SENDING_IMPORTED).where(TEMP_SENDING_IMPORTED.CREATED_BY.eq(userName))
        val count = query.count()
        val data = query
            .orderBy(TEMP_SENDING_IMPORTED.CREATED_DATE.desc())
            .fetchInto(TempSendingImported::class.java)

        return Pair(data, count)
    }

    fun getListFormCode(): List<String> {
        val offset = OffsetDateTime.now().offset
        val threeDaysAgo = OffsetDateTime.of(LocalDate.now().minusDays(2), LocalTime.MIDNIGHT, offset)
        val tomorrow = OffsetDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT, offset)
        return context.selectDistinct(TEMP_SENDING_IMPORTED.FORM_CODE, TEMP_SENDING_IMPORTED.CREATED_DATE)
            .from(TEMP_SENDING_IMPORTED)
            .where(
                TEMP_SENDING_IMPORTED.FORM_CODE.isNotNull
                    .and(TEMP_SENDING_IMPORTED.CREATED_DATE.ge(threeDaysAgo))
                    .and(TEMP_SENDING_IMPORTED.CREATED_DATE.lt(tomorrow))
            )
            .orderBy(TEMP_SENDING_IMPORTED.CREATED_DATE.desc())
            .fetch(TEMP_SENDING_IMPORTED.FORM_CODE)
            .filterNotNull()
    }

    fun saveAll(dataList: List<TempSendingImported>): Int {
        if (dataList.isEmpty()) return 0

        return context.transactionResult { configuration ->
            val transactionalContext = DSL.using(configuration)

            val result = transactionalContext.batchInsert(
                dataList.map { data ->
                    TEMP_SENDING_IMPORTED.newRecord().apply {
                        this.inspectionDate = data.inspectionDate
                        this.locationCode = data.locationCode
                        this.poNumber = data.poNumber
                        this.qty = data.qty
                        this.createdBy = CommonUtils.loggedInUser() ?: Constants.SYSTEM
                        this.formCode = data.formCode
                    }
                }
            ).execute()

            result.sum()
        }
    }

    fun delete(userName: String) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(TEMP_SENDING_IMPORTED).where(TEMP_SENDING_IMPORTED.CREATED_BY.eq(userName))
                .execute()
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "inspectionDate" -> TEMP_SENDING_IMPORTED.INSPECTION_DATE
            "createdDate" -> TEMP_SENDING_IMPORTED.CREATED_DATE
            else -> TEMP_SENDING_IMPORTED.CREATED_DATE
        }
        return sortField
    }
}