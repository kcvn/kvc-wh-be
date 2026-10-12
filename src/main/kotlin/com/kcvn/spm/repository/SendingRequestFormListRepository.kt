package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.SendingFormList
import com.kcvn.spm.model.tables.references.SENDING_FORM_LIST
import com.kcvn.spm.model.tables.references.SENDING_PICKING_LIST
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class SendingRequestFormListRepository(private val context: DSLContext) : SortingRepository() {

    fun findByFormCode(formCode: String): SendingFormList? {
        return context.selectFrom(SENDING_FORM_LIST)
            .where(
                SENDING_FORM_LIST.FORM_CODE.eq(formCode)
            )
            .fetchOneInto(SendingFormList::class.java)
    }

    fun save(data: SendingFormList): String {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        val record = context.newRecord(SENDING_FORM_LIST, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }
        return context.insertInto(SENDING_FORM_LIST)
            .set(record)
            .returning(SENDING_FORM_LIST.ID)
            .fetchOne()!!.id!!
    }

    fun getListFormCode(formStatus: String, isIncludeGe3Days: Boolean): List<String> {
        val offset = OffsetDateTime.now().offset
        val sevenDaysAgo = OffsetDateTime.of(LocalDate.now().minusDays(7), LocalTime.MIDNIGHT, offset)
        val tomorrow = OffsetDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT, offset)

        val condition = SENDING_FORM_LIST.FORM_CODE.isNotNull
            .and(SENDING_FORM_LIST.CREATED_DATE.lt(tomorrow))
            .and(if (formStatus == "ALL") DSL.noCondition() else if (formStatus == "APPROVED") SENDING_FORM_LIST.IS_APPROVED.eq(true) else SENDING_FORM_LIST.IS_APPROVED.eq(false))
            .and(if (isIncludeGe3Days) DSL.noCondition() else SENDING_FORM_LIST.CREATED_DATE.ge(sevenDaysAgo))

        return context.selectDistinct(SENDING_FORM_LIST.FORM_CODE, SENDING_FORM_LIST.CREATED_DATE)
            .from(SENDING_FORM_LIST)
            .where(condition)
            .orderBy(SENDING_FORM_LIST.CREATED_DATE.desc())
            .fetch(SENDING_FORM_LIST.FORM_CODE)
            .filterNotNull()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> SENDING_PICKING_LIST.CREATED_DATE
            else -> SENDING_PICKING_LIST.CREATED_DATE
        }
        return sortField
    }
}