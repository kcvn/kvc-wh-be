package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.SendingFormList
import com.kcvn.spm.model.tables.pojos.SendingPickingList
import com.kcvn.spm.model.tables.pojos.SendingRequestList
import com.kcvn.spm.model.tables.pojos.SendingRequestListDetail
import com.kcvn.spm.model.tables.references.SENDING_FORM_LIST
import com.kcvn.spm.model.tables.references.SENDING_PICKING_LIST
import com.kcvn.spm.model.tables.references.SENDING_REQUEST_LIST
import com.kcvn.spm.model.tables.references.SENDING_REQUEST_LIST_DETAIL
import com.kcvn.spm.model.tables.references.TEMP_SENDING_IMPORTED
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class SendingRequestFormListRepository(private val context: DSLContext) : SortingRepository() {

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

    fun findByFormCode(formCode: String): SendingFormList? {
        return context.selectFrom(SENDING_FORM_LIST)
            .where(
                SENDING_FORM_LIST.FORM_CODE.eq(formCode)
            )
            .fetchOneInto(SendingFormList::class.java)
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