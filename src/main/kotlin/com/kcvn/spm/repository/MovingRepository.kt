package com.kcvn.spm.repository

import com.kcvn.spm.app.moving.payload.request.MovingSearchRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Moving
import com.kcvn.spm.model.tables.references.MOVING
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class MovingRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: MovingSearchRequest, pageable: Pageable, isExport: Boolean = false) : Pair<List<Moving>, Int> {
        var condition: Condition = DSL.noCondition()
        if (!request.sourceLocationCode.isNullOrEmpty()) {
            condition = condition.and(MOVING.SOURCE_LOCATION_CODE.containsIgnoreCase(request.sourceLocationCode!!.trim()))
        }
        if (!request.destLocationCode.isNullOrEmpty()) {
            condition = condition.and(MOVING.DEST_LOCATION_CODE.containsIgnoreCase(request.destLocationCode!!.trim()))
        }
        if (!request.poNumber.isNullOrEmpty()) {
            condition = condition.and(MOVING.PO_NUMBER.containsIgnoreCase(request.poNumber!!.trim()))
        }

        val query = context.selectFrom(MOVING).where(condition)

        if (isExport) {
            val data = query
                .orderBy(getSortFields(pageable.sort, MOVING.SOURCE_LOCATION_CODE))
                .fetchInto(Moving::class.java)

            return Pair(data, data.size)
        } else {
            val count = query.count()
            val data = query
                .orderBy(getSortFields(pageable.sort, MOVING.SOURCE_LOCATION_CODE))
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(Moving::class.java)

            return Pair(data, count)
        }
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