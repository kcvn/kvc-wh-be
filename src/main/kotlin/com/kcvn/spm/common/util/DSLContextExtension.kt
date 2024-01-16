package com.kcvn.spm.common.util

import com.kcvn.spm.common.constants.Constants
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.impl.DSL
import java.io.FileInputStream
import java.util.Properties
import java.sql.Connection
import java.sql.DriverManager

class DSLContextExtension {
    companion object {
        fun createDSLContext(connectionName: String, sqlDialect: SQLDialect): DSLContext {
            val properties = Properties()
            properties.load(FileInputStream(Constants.APPLICATION_PROPERTIES_FILE_URL))

            val url = properties.getProperty("$connectionName.url")
            val user = properties.getProperty("$connectionName.user")
            val password = properties.getProperty("$connectionName.password")

            val connection: Connection = DriverManager.getConnection(url, user, password)
            //val sqlDialect = SQLDialect.POSTGRES

            return DSL.using(connection, sqlDialect)
        }
    }
}