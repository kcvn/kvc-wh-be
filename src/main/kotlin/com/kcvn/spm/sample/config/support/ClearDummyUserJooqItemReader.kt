package com.kcvn.spm.sample.config.support

import com.kcvn.spm.common.batch.db.AbstractJooqItemReader
import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.model.tables.references.AUTH_USER
import org.jooq.DSLContext

open class ClearDummyUserJooqItemReader(private val dslContext: DSLContext) : AbstractJooqItemReader<AuthUser>(dslContext) {
    override fun doOpen() {
        iterator = dslContext.selectFrom(AUTH_USER).where(AUTH_USER.USERNAME.startsWith("dummy")).fetchInto(AuthUser::class.java).iterator()
    }
}