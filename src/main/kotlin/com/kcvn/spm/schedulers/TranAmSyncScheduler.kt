package com.kcvn.spm.schedulers

import com.kcvn.spm.app.sync.service.SyncTransAmDataService
import com.kcvn.spm.common.constants.KeyAppSetting
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.repository.AppSettingRepository
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.util.concurrent.TimeUnit

@EnableScheduling
@Component
class TranAmSyncScheduler(
    private val syncTranAmService: SyncTransAmDataService,
    private val appSettingRep: AppSettingRepository,
) {
    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun runSyncProcessProcedureStructure(){
        println("Start Job SyncProcessProcedureStructure at ${DateTimeHelper.toString(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss")}")
        val timeRunning = appSettingRep.findByKey(KeyAppSetting.SCHEDULER_PROCESS_PROCEDURE_STRUCTURE)

        if (timeRunning == null || timeRunning.value.isNullOrEmpty()) return

        val hour = timeRunning.value!!.split(":")[0].toInt()
        val minute = timeRunning.value!!.split(":")[1].toInt()
        val dt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(7)

        if (dt.hour == hour && dt.minute >= minute && (dt.minute - minute) < 5) {
            try {
                syncTranAmService.syncProcessProcedureStructure()
                println("Complete Sync Data at ${DateTimeHelper.toString(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss")}")
            }catch (e: Exception){
                e.printStackTrace()
            }

        }
        println("End Job SyncProcessProcedureStructure at ${DateTimeHelper.toString(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss")}")
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun runSyncProcessMaster(){
        println("Start Job SyncProcessMaster at ${DateTimeHelper.toString(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss")}")
        val timeRunning = appSettingRep.findByKey(KeyAppSetting.SCHEDULER_PROCESS_MASTER)

        if (timeRunning == null || timeRunning.value.isNullOrEmpty()) return

        val hour = timeRunning.value!!.split(":")[0].toInt()
        val minute = timeRunning.value!!.split(":")[1].toInt()
        val dt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(7)

        if (dt.hour == hour && dt.minute >= minute && (dt.minute - minute) < 5) {
            try {
                syncTranAmService.syncProcessMaster()
                println("Complete Sync Data at ${DateTimeHelper.toString(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss")}")
            }catch (e: Exception){
                e.printStackTrace()
            }

        }
        println("End Job SyncProcessMaster at ${DateTimeHelper.toString(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss")}")
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun runSyncWorkResult(){
        println("Start Job SyncWorkResult at ${DateTimeHelper.toString(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss")}")
        val startTimeConfig = appSettingRep.findByKey(KeyAppSetting.SCHEDULER_WORK_RESULT_START)
        val endTimeConfig = appSettingRep.findByKey(KeyAppSetting.SCHEDULER_WORK_RESULT_END)

        if (startTimeConfig == null || startTimeConfig.value.isNullOrEmpty() || endTimeConfig == null || endTimeConfig.value.isNullOrEmpty()) return

        val dtNow = LocalDateTime.now()

        val startHour = startTimeConfig.value!!.split(":")[0].toInt()
        val startMinute = startTimeConfig.value!!.split(":")[1].toInt()
        val endHour = endTimeConfig.value!!.split(":")[0].toInt()
        val endMinute = endTimeConfig.value!!.split(":")[1].toInt()
        val startTime = LocalDateTime.of(dtNow.year, dtNow.monthValue, dtNow.dayOfMonth, startHour, startMinute, 0)
        val endTime = LocalDateTime.of(dtNow.year, dtNow.monthValue, dtNow.dayOfMonth, endHour, endMinute, 0)

        if (dtNow.isAfter(startTime) && dtNow.isBefore(endTime)) {
            try {
                syncTranAmService.syncWorkResult()
                println("Complete Sync Data at ${DateTimeHelper.toString(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss")}")
            }catch (e: Exception){
                e.printStackTrace()
            }

        }
        println("End Job SyncWorkResult at ${DateTimeHelper.toString(LocalDateTime.now(), "yyyy-MM-dd HH:mm:ss")}")
    }
}