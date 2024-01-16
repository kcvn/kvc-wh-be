package com.kcvn.spm.schedulers

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.KeyAppSetting
import com.kcvn.spm.common.util.DSLContextExtension
import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.model.tables.references.AUTH_USER
import com.kcvn.spm.repository.AppSettingRepository
import org.jooq.SQLDialect
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit


@EnableScheduling
@Component
class DemoSyncScheduler(
    private val appSettingRep: AppSettingRepository
) {
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun runScheduledTask() {
        val timeRunning = appSettingRep.findByKey(KeyAppSetting.SCHEDULER_DEMO_SYNC_TIME_RUNNING)

        if (timeRunning == null || timeRunning?.value.isNullOrEmpty()) return

        val hour = timeRunning.value!!.split(":")[0].toInt()
        val minute = timeRunning.value!!.split(":")[1].toInt()

        val dt = LocalDateTime.now(ZoneOffset.UTC).plusHours(7)
        if (dt.hour == hour && dt.minute >= minute && (dt.minute - minute) <= 5) {
            val dslContext = DSLContextExtension.createDSLContext(Constants.SYNC_DB_CONNECTION_NAME, SQLDialect.POSTGRES)
            val users = dslContext.selectFrom(AUTH_USER).fetchInto(AuthUser::class.java)
            if (users.size > 0) {
                users.forEach { user: AuthUser -> println(user.username) }
            } else {
                println("1")
            }
        } else {
            println("No data")
        }
    }
}