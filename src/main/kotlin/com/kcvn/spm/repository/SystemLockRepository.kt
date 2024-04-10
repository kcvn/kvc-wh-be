package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.SystemLock
import com.kcvn.spm.model.tables.references.SYSTEM_LOCK
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class SystemLockRepository(private val context: DSLContext) {

    fun lock(types: List<String>) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val query = types.map { x -> transactionalContext.insertInto(SYSTEM_LOCK, SYSTEM_LOCK.TYPE, SYSTEM_LOCK.IS_LOCK).values(x, true) }
            transactionalContext.batch(query).execute()
        }
    }

    fun unlock(types: List<String>) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val query = types.map { x -> transactionalContext.deleteFrom(SYSTEM_LOCK).where(SYSTEM_LOCK.TYPE.eq(x)) }
            transactionalContext.batch(query).execute()
        }
    }

    fun isLock(type: String): Boolean {
        val data = context.selectFrom(SYSTEM_LOCK)
            .where(SYSTEM_LOCK.TYPE.eq(type).and(SYSTEM_LOCK.IS_DELETED.eq(false)))
            .fetchInto(SystemLock::class.java)
            .firstOrNull()

        return data?.isLock ?: false
    }
}