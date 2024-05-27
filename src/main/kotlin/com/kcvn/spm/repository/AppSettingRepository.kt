package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.AppSetting
import com.kcvn.spm.model.tables.references.APP_SETTING
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class AppSettingRepository(private val context: DSLContext) {

    fun findByKey(key: String): AppSetting? {
        return context.selectFrom(APP_SETTING).where(APP_SETTING.KEY.eq(key))
            .and(APP_SETTING.IS_DELETED.eq(false))
            .fetchInto(AppSetting::class.java)
            .firstOrNull()
    }

    fun add(appSetting: AppSetting): AppSetting {
        context.insertInto(APP_SETTING)
            .set(APP_SETTING.KEY, appSetting.key)
            .set(APP_SETTING.VALUE, appSetting.value)
            .set(APP_SETTING.DESCRIPTION, appSetting.description)
            .execute()
        return appSetting
    }
}