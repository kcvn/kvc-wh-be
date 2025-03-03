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
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class MovingRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: MovingSearchRequest, pageable: Pageable, isExport: Boolean = false) : Pair<List<Moving>, Int> {
        var condition: Condition = DSL.noCondition()
        if(!request.listSourceLocationCode.isNullOrEmpty()){
            val sourceLocationCodes = request.listSourceLocationCode!!.split(",")
            var condition1 : Condition = DSL.noCondition()
            sourceLocationCodes.forEach { sourceLocationCode ->
                condition1 = condition1.or(MOVING.SOURCE_LOCATION_CODE.eq(sourceLocationCode.trim()))
            }
            condition = condition.and(condition1)
        }
        if(!request.listDestLocationCode.isNullOrEmpty()){
            val destLocationCodes = request.listDestLocationCode!!.split(",")
            var condition1 : Condition = DSL.noCondition()
            destLocationCodes.forEach { destLocationCode ->
                condition1 = condition1.or(MOVING.DEST_LOCATION_CODE.eq(destLocationCode.trim()))
            }
            condition = condition.and(condition1)
        }
        if (!request.poNumber.isNullOrEmpty()) {
            condition = condition.and(MOVING.PO_NUMBER.containsIgnoreCase(request.poNumber!!.trim()))
        }
        if (request.fromDate != null && request.toDate != null)
            condition = condition.and(MOVING.CREATED_DATE.between(request.fromDate, request.toDate))

        val query = context.selectFrom(MOVING).where(condition.and(MOVING.IS_CANCELED.eq(false)))

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

    fun updateIsCanceled(data: Moving) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(MOVING)
                .set(MOVING.IS_CANCELED, true)
                .set(MOVING.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(MOVING.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(
                    MOVING.SOURCE_LOCATION_CODE.eq(data.sourceLocationCode)
                    .and(MOVING.DEST_LOCATION_CODE.eq(data.destLocationCode))
                    .and(MOVING.PO_NUMBER.eq(data.poNumber))
                    .and(MOVING.SEQ_NO.eq(data.seqNo))
                    .and(MOVING.IS_CANCELED.eq(false))
                )
                .execute()
        }
    }

    fun findMoving(sourceLocationCode: String, destLocationCode: String, poNumber: String, qty: Int, seqNo: Int, receivingSeqNo: Int): Moving? {
        return context.selectFrom(MOVING)
            .where(
                MOVING.SOURCE_LOCATION_CODE.eq(sourceLocationCode)
                    .and(MOVING.DEST_LOCATION_CODE.eq(destLocationCode))
                    .and(MOVING.PO_NUMBER.eq(poNumber))
                    .and(MOVING.QTY.eq(qty))
                    .and(MOVING.SEQ_NO.eq(seqNo))
                    .and(MOVING.RECEIVING_SEQ_NO.eq(receivingSeqNo))
            )
            .fetchInto(Moving::class.java)
            .firstOrNull()
    }

    fun findLatestMoving(sourceLocationCode: String, destLocationCode: String, poNumber: String, todayUtc: LocalDate ): Moving? {
        return context.selectFrom(MOVING)
            .where(
                MOVING.SOURCE_LOCATION_CODE.eq(sourceLocationCode)
                    .and(MOVING.DEST_LOCATION_CODE.eq(destLocationCode))
                    .and(MOVING.PO_NUMBER.eq(poNumber))
                    .and(MOVING.CREATED_DATE.cast(LocalDate::class.java).eq(todayUtc))
            )
            .orderBy(MOVING.SEQ_NO.sort(SortOrder.DESC))
            .fetchInto(Moving::class.java)
            .firstOrNull()
    }

    fun save(rec: Moving) {
        context.insertInto(
            MOVING, MOVING.SOURCE_LOCATION_CODE, MOVING.DEST_LOCATION_CODE, MOVING.PO_NUMBER,
            MOVING.QTY, MOVING.SEQ_NO, MOVING.CREATED_BY, MOVING.RECEIVING_SEQ_NO)
            .values(rec.sourceLocationCode, rec.destLocationCode, rec.poNumber, rec.qty, rec.seqNo, CommonUtils.loggedInUser() ?: Constants.SYSTEM, rec.receivingSeqNo)
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