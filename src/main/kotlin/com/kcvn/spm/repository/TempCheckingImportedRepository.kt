package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.TempCheckingImported
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
class TempCheckingImportedRepository(private val context: DSLContext) : SortingRepository() {
    fun findByFormCode(formCode: String): TempCheckingImported? {
        return context.selectFrom(TEMP_CHECKING_IMPORTED)
            .where(
                TEMP_CHECKING_IMPORTED.FORM_CODE.eq(formCode)
            )
            .fetchInto(TempCheckingImported::class.java)
            .firstOrNull()
    }

    fun getListFormCode(formStatus: String, isIncludeGe3Days: Boolean): List<String> {
        val offset = OffsetDateTime.now().offset
        val threeDaysAgo = OffsetDateTime.of(LocalDate.now().minusDays(2), LocalTime.MIDNIGHT, offset)
        val tomorrow = OffsetDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT, offset)
        return context.selectDistinct(TEMP_CHECKING_IMPORTED.FORM_CODE, TEMP_CHECKING_IMPORTED.CREATED_DATE)
            .from(TEMP_CHECKING_IMPORTED)
            .where(
                TEMP_CHECKING_IMPORTED.FORM_CODE.isNotNull
                    //.and(TEMP_CHECKING_IMPORTED.CREATED_DATE.ge(threeDaysAgo))
                    //.and(if (formStatus == "ALL") DSL.noCondition() else if (formStatus == "APPROVED") TEMP_CHECKING_IMPORTED.IS_APPROVED.eq(true) else TEMP_CHECKING_IMPORTED.IS_APPROVED.eq(false))
                    .and(TEMP_CHECKING_IMPORTED.CREATED_DATE.lt(tomorrow))
                    .and(if (isIncludeGe3Days) DSL.noCondition() else TEMP_CHECKING_IMPORTED.CREATED_DATE.ge(threeDaysAgo))
            )
            .orderBy(TEMP_CHECKING_IMPORTED.CREATED_DATE.desc())
            .fetch(TEMP_CHECKING_IMPORTED.FORM_CODE)
            .filterNotNull()
    }

    fun getList(formCode: String) : Pair<List<TempCheckingImported>, Int> {
        val query = context.selectFrom(TEMP_CHECKING_IMPORTED)
            .where(TEMP_CHECKING_IMPORTED.FORM_CODE.eq(formCode))
        val count = query.count()
        val data = query
            .orderBy(TEMP_CHECKING_IMPORTED.CREATED_DATE.desc())
            .fetchInto(TempCheckingImported::class.java)

        return Pair(data, count)
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