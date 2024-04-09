package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.ExportConfiguration
import com.kcvn.spm.model.tables.pojos.UpdateTape
import com.kcvn.spm.model.tables.references.EXPORT_CONFIGURATION
import com.kcvn.spm.model.tables.references.UPDATE_TAPE
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository

class ExportConfigurationRepository (private val context: DSLContext){
    fun getExportConfig(productNames:  List<String?>, startDate: OffsetDateTime?, endDate: OffsetDateTime?): List<ExportConfiguration> {
        var condition = DSL.noCondition().and(EXPORT_CONFIGURATION.IS_DELETED.eq(false))
        if (endDate != null) {
            condition= condition.and(EXPORT_CONFIGURATION.EXPIRATION_DATE.le(endDate))
        }
        if (startDate != null) {
            condition= condition.and(EXPORT_CONFIGURATION.EFFECTIVE_DATE.ge(startDate))
        }
        return context.selectFrom(EXPORT_CONFIGURATION)
            .where(condition.and(EXPORT_CONFIGURATION.PRODUCT_NAME.`in`(productNames)))
            .fetchInto(ExportConfiguration::class.java)
    }
}