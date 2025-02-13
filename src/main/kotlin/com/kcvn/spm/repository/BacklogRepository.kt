package com.kcvn.spm.repository

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
    companion object {
        const val PERMISSION_TYPE = "permission"
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

    fun findByKeywordPaginated(keyword: String?, pageable: Pageable): Pair<List<Backlog>, Int> {
        var condition: Condition = DSL.noCondition()
        if (keyword != null) {
            condition = condition.and(BACKLOG.LOCATION_CODE.containsIgnoreCase(keyword))
        }
        val backlog = context.selectFrom(BACKLOG).where(condition)
            .orderBy(getSortFields(pageable.sort, BACKLOG.CREATED_DATE))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(Backlog::class.java)
        val total = context.fetchCount(BACKLOG, condition)
        return Pair(backlog, total)
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