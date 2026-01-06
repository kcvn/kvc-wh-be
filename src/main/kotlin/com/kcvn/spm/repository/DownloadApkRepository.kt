package com.kcvn.spm.repository

import com.kcvn.spm.app.checkinghistory.payload.request.CheckingHistorySearchRequest
import com.kcvn.spm.app.download.payload.ApkVersionCheckingRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ApkVersion
import com.kcvn.spm.model.tables.pojos.CheckingHistory
import com.kcvn.spm.model.tables.pojos.Moving
import com.kcvn.spm.model.tables.references.*
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class DownloadApkRepository(private val context: DSLContext) : SortingRepository() {

    fun findOneRecordByDeviceName(data: ApkVersionCheckingRequest): ApkVersion?{
        return context.selectFrom(APK_VERSION)
            .where(
                APK_VERSION.DEVICE_NAME.eq(data.deviceName)
            )
            .fetchInto(ApkVersion::class.java)
            .firstOrNull()
    }

    fun updateVersionData(data: ApkVersionCheckingRequest){
        context.update(APK_VERSION)
            .set(APK_VERSION.CURRENT_VERSION, data.currentVersion)
            .where(
                APK_VERSION.DEVICE_NAME.eq(data.deviceName)
            )
            .execute()
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "createdDate" -> APK_VERSION.CREATED_TIME
            else -> APK_VERSION.CREATED_TIME
        }
        return sortField
    }
}