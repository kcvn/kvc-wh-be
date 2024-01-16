package com.kcvn.spm.common.batch.db

import org.apache.commons.logging.LogFactory
import org.jooq.DSLContext
import org.springframework.batch.item.ItemWriter
import org.springframework.beans.factory.InitializingBean


abstract class AbstractJooqItemWriter<T>(private val dsl: DSLContext) : ItemWriter<T>, InitializingBean {
    protected val logger = LogFactory.getLog(javaClass)

    override fun afterPropertiesSet() {
        // noop
    }

}