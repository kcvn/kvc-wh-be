package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.AppSetting
import com.kcvn.spm.model.tables.references.APP_SETTING
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AppSettingRepository(private val context: DSLContext) {

    fun findById(id: String): AppSetting? {
        return context.selectFrom(APP_SETTING).where(APP_SETTING.ID.eq(id)).fetchInto(AppSetting::class.java).firstOrNull()
    }

    fun findByKey(key: String): AppSetting? {
        return context.selectFrom(APP_SETTING).where(APP_SETTING.KEY.eq(key))
            .and(APP_SETTING.IS_DELETED.eq(false))
            .fetchInto(AppSetting::class.java)
            .firstOrNull()
    }
}