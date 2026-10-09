package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.SendingPickingList
import com.kcvn.spm.model.tables.pojos.SendingRequestListDetail
import com.kcvn.spm.model.tables.references.SENDING_PICKING_LIST
import com.kcvn.spm.model.tables.references.SENDING_REQUEST_LIST
import com.kcvn.spm.model.tables.references.SENDING_REQUEST_LIST_DETAIL
import org.jooq.DSLContext
import org.jooq.TableField
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class SendingPickingListRepository(private val context: DSLContext) : SortingRepository() {
    fun findAllByPORecDateAndLot(poNumber: String, recDate: LocalDate, lotNo: String): List<SendingPickingList>? {
        return context.selectFrom(SENDING_PICKING_LIST)
            .where(
                SENDING_PICKING_LIST.PO_NUMBER.eq(poNumber)
                    .and (SENDING_PICKING_LIST.RECEIVING_DATE.eq(recDate))
                    .and (SENDING_PICKING_LIST.LOT_NO.eq(lotNo))
                    .and (SENDING_PICKING_LIST.IS_DELETED.eq(false))
            )
            .fetchInto(SendingPickingList::class.java)
    }

    fun saveAll(dataList: List<SendingPickingList>) {
        if (dataList.isEmpty()) return
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        val records = dataList.map { data ->
            context.newRecord(SENDING_PICKING_LIST, data).apply {
                createdBy = user
                createdDate = currentTime
                updatedBy = user
                updatedDate = currentTime
            }
        }
        context.batchInsert(records).execute()
    }

    fun deleteAllBySendingRequestId(sendingRequestId: String) {
        if (sendingRequestId.isEmpty()) return
        val currentTime = LocalDateTime.now().atOffset(ZoneOffset.UTC)
        val user = CommonUtils.loggedInUser() ?: Constants.SYSTEM

        context.update(SENDING_PICKING_LIST)
            .set(SENDING_PICKING_LIST.IS_DELETED, true)
            .set(SENDING_PICKING_LIST.UPDATED_DATE, currentTime)
            .set(SENDING_PICKING_LIST.UPDATED_BY, user)
            .where(SENDING_PICKING_LIST.SENDING_REQUEST_ID.eq(sendingRequestId))
            .execute()
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