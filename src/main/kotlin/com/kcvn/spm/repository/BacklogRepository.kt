package com.kcvn.spm.repository

import com.kcvn.spm.app.backlog.payload.request.BacklogSearchRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Backlog
import com.kcvn.spm.model.tables.references.BACKLOG
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class BacklogRepository(private val context: DSLContext) : SortingRepository() {
    fun getList(request: BacklogSearchRequest, pageable: Pageable, isExport: Boolean = false) : Pair<List<Backlog>, Int> {
        var condition: Condition = DSL.noCondition()
        if(!request.locationCode.isNullOrEmpty()){
            val locationCodes = request.locationCode!!.split(",")
            var condition1 : Condition = DSL.noCondition()
            locationCodes.forEach { lc ->
                condition1 = condition1.or(BACKLOG.LOCATION_CODE.eq(lc.trim()))
            }
            condition = condition.and(condition1)
        }
        if (!request.poNumber.isNullOrEmpty()) {
            condition = condition.and(BACKLOG.PO_NUMBER.containsIgnoreCase(request.poNumber!!.trim()))
        }

        val query = context.selectFrom(BACKLOG).where(condition)

        if (isExport) {
            val data = query
                .orderBy(getSortFields(pageable.sort, BACKLOG.LOCATION_CODE))
                .fetchInto(Backlog::class.java)

            return Pair(data, data.size)
        } else {
            val count = query.count()
            val data = query
                .orderBy(getSortFields(pageable.sort, BACKLOG.LOCATION_CODE))
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(Backlog::class.java)

            return Pair(data, count)
        }
    }

    fun save(rec: Backlog) {
        context.insertInto(
            BACKLOG, BACKLOG.LOCATION_CODE, BACKLOG.PO_NUMBER,
            BACKLOG.BACKLOG_QTY, BACKLOG.BOX_QTY, BACKLOG.CREATED_BY)
            .values(rec.locationCode, rec.poNumber, rec.backlogQty, rec.boxQty, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .execute()
    }

    fun update(data: Backlog) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)

            transactionalContext.update(BACKLOG)
                .set(BACKLOG.BACKLOG_QTY, data.backlogQty)
                .set(BACKLOG.BOX_QTY, data.boxQty)
                .set(BACKLOG.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(BACKLOG.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(BACKLOG.LOCATION_CODE.eq(data.locationCode).and(BACKLOG.PO_NUMBER.eq(data.poNumber)))
                .execute()
        }
    }

    fun findByLocationCodeAndPO(locationCode: String, poNumber: String): Backlog? {
        return context.selectFrom(BACKLOG)
            .where(BACKLOG.LOCATION_CODE.eq(locationCode).and(BACKLOG.PO_NUMBER.eq(poNumber)))
            .fetchInto(Backlog::class.java)
            .firstOrNull()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "locationCode" -> BACKLOG.LOCATION_CODE
            "createdDate" -> BACKLOG.CREATED_DATE
            else -> BACKLOG.CREATED_DATE
        }
        return sortField
    }
}