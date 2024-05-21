package com.kcvn.spm.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource


@Configuration
@PropertySource("classpath:application.properties")
class PropertiesConfig {

    @Value("\${trans_am_db.url}")
    lateinit var tranAmDbUrl: String

    @Value("\${trans_am_db.user}")
    lateinit var tranAmDbUser: String

    @Value("\${trans_am_db.password}")
    lateinit var tranAmDbPassword: String

    @Value("\${trans_am_db.schema}")
    lateinit var tranAmDbSchema: String

    @Value("\${trans_am_db.hasSchema}")
    lateinit var tranAmDbHasSchema: String

}