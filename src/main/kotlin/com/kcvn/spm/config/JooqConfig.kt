package com.kcvn.spm.config

import org.jooq.SQLDialect
import org.jooq.impl.DataSourceConnectionProvider
import org.jooq.impl.DefaultConfiguration
import org.jooq.impl.DefaultDSLContext
import org.jooq.impl.DefaultExecuteListenerProvider
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.jdbc.datasource.TransactionAwareDataSourceProxy
import javax.sql.DataSource


@Configuration
class JooqConfig(private val dataSource: DataSource) {
    @Bean
    fun connectionProvider(): DataSourceConnectionProvider {
        return DataSourceConnectionProvider(TransactionAwareDataSourceProxy(dataSource))
    }

    @Bean
    fun dsl(): DefaultDSLContext {
        return DefaultDSLContext(configuration())
    }

    fun configuration(): DefaultConfiguration {
        val jooqConfiguration = DefaultConfiguration()
        jooqConfiguration.set(connectionProvider())
        jooqConfiguration.set(DefaultExecuteListenerProvider(ExceptionTranslator()))
        jooqConfiguration.set(SQLDialect.POSTGRES)
        return jooqConfiguration
    }
}