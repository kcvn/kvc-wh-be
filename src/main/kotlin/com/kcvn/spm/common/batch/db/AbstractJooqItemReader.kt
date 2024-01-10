package com.kcvn.spm.common.batch.db

import org.jooq.DSLContext
import org.springframework.batch.item.support.AbstractItemCountingItemStreamItemReader
import org.springframework.beans.factory.InitializingBean
import org.springframework.util.ClassUtils

abstract class AbstractJooqItemReader<T>(private val dsl: DSLContext) : AbstractItemCountingItemStreamItemReader<T>(), InitializingBean {
    protected var iterator: Iterator<T>? = null

    init {
        this.name = ClassUtils.getShortName(this.javaClass)
    }

    override fun doRead(): T? {
        if (iterator == null) return null
        return if (iterator!!.hasNext()) iterator!!.next() else null
    }

    override fun doClose() {
        // noop
    }

    override fun afterPropertiesSet() {
        // noop
    }
}