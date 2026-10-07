package com.kcvn.spm.repository

import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchForSendingRequest
import com.kcvn.spm.app.backlogwh.payload.request.BacklogWhSearchRequest
import com.kcvn.spm.app.backlogwh.payload.request.ImportBacklogWh
import com.kcvn.spm.app.backlogwh.payload.response.BacklogWhResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.BacklogWh
import com.kcvn.spm.model.tables.pojos.NewBacklogWh
import com.kcvn.spm.model.tables.pojos.PurchaseOrderBacklog
import com.kcvn.spm.model.tables.pojos.SendingRequestList
import com.kcvn.spm.model.tables.references.BACKLOG_WH
import com.kcvn.spm.model.tables.references.NEW_BACKLOG_WH
import com.kcvn.spm.model.tables.references.PURCHASE_ORDER_BACKLOG
import com.kcvn.spm.model.tables.references.SENDING_REQUEST_LIST
import com.kcvn.spm.model.tables.references.SPLITTING
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.SQLDataType
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class SendingRequestListRepository(private val context: DSLContext) : SortingRepository() {

    fun findByReceivingDatePO(poNumber: String, receivingDate: LocalDate): SendingRequestList? {
        return context.selectFrom(SENDING_REQUEST_LIST)
            .where(
                SENDING_REQUEST_LIST.PO_NUMBER.eq(poNumber)
                    .and (SENDING_REQUEST_LIST.RECEIVING_DATE.eq(receivingDate))
            )
            .orderBy(SENDING_REQUEST_LIST.SEQ_NO.desc())
            .fetchInto(SendingRequestList::class.java)
            .firstOrNull()
    }

    fun save(data: SendingRequestList): String {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        val record = context.newRecord(SENDING_REQUEST_LIST, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }
        return context.insertInto(SENDING_REQUEST_LIST)
            .set(record)
            .returning(SENDING_REQUEST_LIST.ID)
            .fetchOne()!!.id!!
    }
    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> SENDING_REQUEST_LIST.CREATED_DATE
            else -> SENDING_REQUEST_LIST.CREATED_DATE
        }
        return sortField
    }
}