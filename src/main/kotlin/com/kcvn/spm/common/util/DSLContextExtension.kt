package com.kcvn.spm.common.util

import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import java.sql.Connection
import java.sql.DriverManager

class DSLContextExtension {
    companion object {
        fun createDSLContext(url: String, user: String, password: String, sqlDialect: SQLDialect): DSLContext {
            val connection: Connection = DriverManager.getConnection(url, user, password)
            return DSL.using(connection, sqlDialect)
        }
    }
}