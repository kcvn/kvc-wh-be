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

@Repository
class MovingRepository(private val context: DSLContext) : SortingRepository() {
    companion object {
        const val PERMISSION_TYPE = "permission"
    }

    fun findMoving(sourceLocationCode: String, destLocationCode: String, poNumber: String, qty: Int, seqNo: Int): Moving? {
        return context.selectFrom(MOVING)
            .where(
                MOVING.SOURCE_LOCATION_CODE.eq(sourceLocationCode)
                    .and(MOVING.DEST_LOCATION_CODE.eq(destLocationCode))
                    .and(MOVING.PO_NUMBER.eq(poNumber))
                    .and(MOVING.QTY.eq(qty))
                    .and(MOVING.SEQ_NO.eq(seqNo)))
            .fetchInto(Moving::class.java)
            .firstOrNull()
    }

    fun findLatestMoving(sourceLocationCode: String, destLocationCode: String, poNumber: String): Moving? {
        return context.selectFrom(MOVING)
            .where(
                MOVING.SOURCE_LOCATION_CODE.eq(sourceLocationCode)
                    .and(MOVING.DEST_LOCATION_CODE.eq(destLocationCode))
                    .and(MOVING.PO_NUMBER.eq(poNumber))
            )
            .orderBy(MOVING.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(Moving::class.java)
            .firstOrNull()
    }

    fun save(rec: Moving) {
        context.insertInto(
            MOVING, MOVING.SOURCE_LOCATION_CODE, MOVING.DEST_LOCATION_CODE, MOVING.PO_NUMBER,
            MOVING.QTY, MOVING.SEQ_NO, MOVING.CREATED_BY)
            .values(rec.sourceLocationCode, rec.destLocationCode, rec.poNumber, rec.qty, rec.seqNo, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> MOVING.DEST_LOCATION_CODE
            "createdDate" -> MOVING.CREATED_DATE
            else -> MOVING.CREATED_DATE
        }
        return sortField
    }
}