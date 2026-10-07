package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.SendingRequestListDetail
import com.kcvn.spm.model.tables.references.SENDING_REQUEST_LIST
import com.kcvn.spm.model.tables.references.SENDING_REQUEST_LIST_DETAIL
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class SendingRequestListDetailRepository(private val context: DSLContext) : SortingRepository() {

    fun findProcessingRequestByReceivingDatePOAndLot(receivingDate: LocalDate, poNumber: String, lotNo: String): List<SendingRequestListDetail>? {
        // Chỉ lấy detail của các request chưa ở status 3, 4
        return context.select(SENDING_REQUEST_LIST_DETAIL.asterisk())
            .from(SENDING_REQUEST_LIST_DETAIL)
            .innerJoin(SENDING_REQUEST_LIST)
            .on(SENDING_REQUEST_LIST.ID.eq(SENDING_REQUEST_LIST_DETAIL.SENDING_REQUEST_ID))
            .where(
                SENDING_REQUEST_LIST_DETAIL.PO_NUMBER.eq(poNumber)
                    .and (SENDING_REQUEST_LIST_DETAIL.RECEIVING_DATE.eq(receivingDate))
                    .and (SENDING_REQUEST_LIST_DETAIL.LOT_NO.eq(lotNo))
                    .and (SENDING_REQUEST_LIST.STATUS.notIn(3, 4))
            )
            .fetchInto(SendingRequestListDetail::class.java)
    }

    fun save(data: SendingRequestListDetail) {
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.newRecord(SENDING_REQUEST_LIST_DETAIL, data).apply {
            createdBy = user
            createdDate = currentTime
            updatedBy = user
            updatedDate = currentTime
        }.insert()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> SENDING_REQUEST_LIST_DETAIL.CREATED_DATE
            else -> SENDING_REQUEST_LIST_DETAIL.CREATED_DATE
        }
        return sortField
    }
}