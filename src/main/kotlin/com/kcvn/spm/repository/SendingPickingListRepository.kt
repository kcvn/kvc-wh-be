package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.SendingPickingList
import com.kcvn.spm.model.tables.pojos.SendingRequestListDetail
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
class SendingPickingListRepository(private val context: DSLContext) : SortingRepository() {
    // Chỉ lấy picking đang giữ hàng: bỏ picking của request đã decline (3) / hoàn tất (4), giống điều kiện processing bên backlog
    fun findAllByPORecDateAndLot(poNumber: String, recDate: LocalDate, lotNo: String): List<SendingPickingList>? {
        return context.select(SENDING_PICKING_LIST.asterisk())
            .from(SENDING_PICKING_LIST)
            .innerJoin(SENDING_REQUEST_LIST_DETAIL)
            .on(SENDING_REQUEST_LIST_DETAIL.ID.eq(SENDING_PICKING_LIST.SENDING_REQUEST_DETAIL_ID))
            .innerJoin(SENDING_REQUEST_LIST)
            .on(SENDING_REQUEST_LIST.ID.eq(SENDING_REQUEST_LIST_DETAIL.SENDING_REQUEST_ID))
            .where(
                SENDING_PICKING_LIST.PO_NUMBER.eq(poNumber)
                    .and (SENDING_PICKING_LIST.RECEIVING_DATE.eq(recDate))
                    .and (SENDING_PICKING_LIST.LOT_NO.eq(lotNo))
                    .and (SENDING_PICKING_LIST.IS_DELETED.eq(false))
                    .and (SENDING_REQUEST_LIST.STATUS.notIn(3, 4))
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

//    fun getListFormCode(formStatus: String, isIncludeGe3Days: Boolean): List<String> {
//        val offset = OffsetDateTime.now().offset
//        val threeDaysAgo = OffsetDateTime.of(LocalDate.now().minusDays(2), LocalTime.MIDNIGHT, offset)
//        val tomorrow = OffsetDateTime.of(LocalDate.now().plusDays(1), LocalTime.MIDNIGHT, offset)
//
//        val condition = SENDING_PICKING_LIST.FORM_CODE.isNotNull
//            .and(SENDING_PICKING_LIST.CREATED_DATE.lt(tomorrow))
//            .and(if (formStatus == "ALL") DSL.noCondition() else if (formStatus == "APPROVED") TEMP_SENDING_IMPORTED.IS_APPROVED.eq(true) else TEMP_SENDING_IMPORTED.IS_APPROVED.eq(false))
//            .and(if (isIncludeGe3Days) DSL.noCondition() else TEMP_SENDING_IMPORTED.CREATED_DATE.ge(threeDaysAgo))
//
//        return context.selectDistinct(TEMP_SENDING_IMPORTED.FORM_CODE, TEMP_SENDING_IMPORTED.CREATED_DATE)
//            .from(TEMP_SENDING_IMPORTED)
//            .where(condition)
//            .orderBy(TEMP_SENDING_IMPORTED.CREATED_DATE.desc())
//            .fetch(TEMP_SENDING_IMPORTED.FORM_CODE)
//            .filterNotNull()
//    }


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> SENDING_PICKING_LIST.CREATED_DATE
            else -> SENDING_PICKING_LIST.CREATED_DATE
        }
        return sortField
    }
}