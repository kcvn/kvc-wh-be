package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.SyncHistory
import com.kcvn.spm.model.tables.references.SYNC_HISTORY
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.springframework.stereotype.Repository

@Repository
class SyncHistoryRepository (private val context: DSLContext) {
    fun findByType(type: String): SyncHistory? {
        return context.selectFrom(SYNC_HISTORY).where(SYNC_HISTORY.TYPE.eq(type).and(SYNC_HISTORY.IS_DELETED.eq(false)))
            .orderBy(SYNC_HISTORY.CREATED_DATE.sort(SortOrder.DESC))
            .fetchInto(SyncHistory::class.java)
            .firstOrNull()
    }

}