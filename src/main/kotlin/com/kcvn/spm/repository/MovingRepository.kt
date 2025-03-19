package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Moving
import com.kcvn.spm.model.tables.references.MOVING
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.springframework.stereotype.Repository
import java.time.LocalDate

@Repository
class MovingRepository(private val context: DSLContext) : SortingRepository() {
    fun saveMoving(moving: Moving) {
        context.insertInto(
            MOVING, MOVING.SOURCE_LOCATION_CODE, MOVING.DEST_LOCATION_CODE,
            MOVING.SOURCE_PACKAGE_CODE, MOVING.DEST_PACKAGE_CODE, MOVING.PO_NUMBER,
            MOVING.QTY, MOVING.SEQ_NO, MOVING.TRANSACTION_TYPE, MOVING.RECEIVING_DATE, MOVING.CREATED_BY
        )
            .values(
                moving.sourceLocationCode, moving.destLocationCode, moving.sourcePackageCode, moving.destPackageCode, moving.poNumber, moving.qty, moving.seqNo, moving.transactionType, moving.receivingDate, CommonUtils.loggedInUser() ?: Constants.SYSTEM
            )
            .execute()
    }

    fun findLatestMoving(sourceLocationCode: String, sourcePackageCode: String, poNumber: String, todayUtc: LocalDate): Moving? {
        return context.selectFrom(MOVING)
            .where(
                MOVING.SOURCE_LOCATION_CODE.eq(sourceLocationCode)
                    .and(MOVING.SOURCE_PACKAGE_CODE.eq(sourcePackageCode))
                    .and(MOVING.PO_NUMBER.eq(poNumber))
                    .and(MOVING.CREATED_DATE.cast(LocalDate::class.java).eq(todayUtc))
            )
            .orderBy(MOVING.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(Moving::class.java)
            .firstOrNull()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> MOVING.SOURCE_LOCATION_CODE
            "createdDate" -> MOVING.CREATED_DATE
            else -> MOVING.CREATED_DATE
        }
        return sortField
    }
}