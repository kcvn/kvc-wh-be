package com.kcvn.spm.sample.config.support

import com.kcvn.spm.common.batch.db.AbstractJooqItemWriter
import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.model.tables.references.AUTH_USER
import org.jooq.DSLContext
import org.springframework.batch.item.Chunk
import org.springframework.beans.factory.InitializingBean

class ClearDummyUserJooqItemWriter(private val dsl: DSLContext) : AbstractJooqItemWriter<AuthUser?>(dsl), InitializingBean {
    override fun write(chunk: Chunk<out AuthUser?>) {
        for (person in chunk) {
            logger.info("delete ${person!!.username}")
            dsl.deleteFrom(AUTH_USER).where(AUTH_USER.ID.eq(person.id)).execute()
        }
//        dsl.deleteFrom(AUTH_USER).where(AUTH_USER.ID.`in`(chunk.map { it!!.id })).execute()
    }
}