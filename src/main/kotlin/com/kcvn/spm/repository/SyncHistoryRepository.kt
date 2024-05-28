package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.SyncHistory
import com.kcvn.spm.model.tables.references.SYNC_HISTORY
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class SyncHistoryRepository(private val context: DSLContext) {
    fun findByType(type: String): SyncHistory? {
        return context.selectFrom(SYNC_HISTORY).where(SYNC_HISTORY.TYPE.eq(type).and(SYNC_HISTORY.IS_DELETED.eq(false)))
            .orderBy(SYNC_HISTORY.CREATED_DATE.sort(SortOrder.DESC))
            .fetchInto(SyncHistory::class.java)
            .firstOrNull()
    }

    fun add(data: SyncHistory, createDate: OffsetDateTime? = null): SyncHistory? {
        val createBy = try {
            CommonUtils.loggedInUser() ?: Constants.SYSTEM
        } catch (e: Exception) {
            Constants.SYSTEM
        }
        if (createDate == null) {
            return context.insertInto(
                SYNC_HISTORY,
                SYNC_HISTORY.SOURCE, SYNC_HISTORY.DESTINATION, SYNC_HISTORY.TYPE, SYNC_HISTORY.CREATED_BY
            ).values(
                data.source, data.destination, data.type, createBy
            ).returningResult(SYNC_HISTORY).fetchInto(SyncHistory::class.java).firstOrNull()
        } else {
            return context.insertInto(
                SYNC_HISTORY,
                SYNC_HISTORY.SOURCE, SYNC_HISTORY.DESTINATION,
                SYNC_HISTORY.TYPE, SYNC_HISTORY.CREATED_DATE, SYNC_HISTORY.CREATED_BY
            ).values(
                data.source, data.destination, data.type, createDate, createBy
            ).returningResult(SYNC_HISTORY).fetchInto(SyncHistory::class.java).firstOrNull()
        }
    }

}