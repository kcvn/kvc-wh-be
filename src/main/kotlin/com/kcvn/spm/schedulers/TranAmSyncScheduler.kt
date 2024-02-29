package com.kcvn.spm.schedulers

import com.kcvn.spm.app.sync.service.SyncTransAmDataService
import com.kcvn.spm.common.constants.KeyAppSetting
import com.kcvn.spm.repository.AppSettingRepository
import org.springframework.scheduling.annotation.EnableScheduling
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
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
        val timeRunning = appSettingRep.findByKey(KeyAppSetting.SCHEDULER_PROCESS_PROCEDURE_STRUCTURE)

        if (timeRunning == null || timeRunning.value.isNullOrEmpty()) return

        val hour = timeRunning.value!!.split(":")[0].toInt()
        val minute = timeRunning.value!!.split(":")[1].toInt()
        val dt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(7)

        if (dt.hour == hour && dt.minute >= minute && (dt.minute - minute) <= 5) {
            println("-------------------------------------------------------")
            println("Scheduled task SyncProcessProcedureStructure started...")
            println("-------------------------------------------------------")
            try {
                syncTranAmService.syncProcessProcedureStructure()
                println("-------------------------------------------------------")
                println("Scheduled task SyncProcessProcedureStructure completed!")
                println("-------------------------------------------------------")
            }catch (e: Exception){
                val messageError = e.message.toString()
                println("-------------------------------------------------------")
                println("Error: $messageError")
                println("-------------------------------------------------------")
            }

        }else {
            println("No data")
        }
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun runSyncProcessMaster(){
        val timeRunning = appSettingRep.findByKey(KeyAppSetting.SCHEDULER_PROCESS_MASTER)

        if (timeRunning == null || timeRunning.value.isNullOrEmpty()) return

        val hour = timeRunning.value!!.split(":")[0].toInt()
        val minute = timeRunning.value!!.split(":")[1].toInt()
        val dt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(7)

        if (dt.hour == hour && dt.minute >= minute && (dt.minute - minute) <= 5) {
            println("-------------------------------------------------------")
            println("Scheduled task SyncProcessMaster started...")
            println("-------------------------------------------------------")
            try {
                syncTranAmService.syncProcessMaster()
                println("-------------------------------------------------------")
                println("Scheduled task SyncProcessMaster completed!")
                println("-------------------------------------------------------")
            }catch (e: Exception){
                val messageError = e.message.toString()
                println("-------------------------------------------------------")
                println("Error: $messageError")
                println("-------------------------------------------------------")
            }

        }else {
            println("No data")
        }
    }

    @Scheduled(fixedDelay = 5, timeUnit = TimeUnit.MINUTES)
    fun runSyncWorkResult(){
        val timeRunning = appSettingRep.findByKey(KeyAppSetting.WORK_RESULT)

        if (timeRunning == null || timeRunning.value.isNullOrEmpty()) return

        val hour = timeRunning.value!!.split(":")[0].toInt()
        val minute = timeRunning.value!!.split(":")[1].toInt()
        val dt = OffsetDateTime.now(ZoneOffset.UTC).plusHours(7)

        if (dt.hour == hour && dt.minute >= minute && (dt.minute - minute) <= 5) {
            println("-------------------------------------------------------")
            println("Scheduled task SyncWorkResult started...")
            println("-------------------------------------------------------")
            try {
                syncTranAmService.syncWorkResult()
                println("-------------------------------------------------------")
                println("Scheduled task SyncWorkResult completed!")
                println("-------------------------------------------------------")
            }catch (e: Exception){
                val messageError = e.message.toString()
                println("-------------------------------------------------------")
                println("Error: $messageError")
                println("-------------------------------------------------------")
            }

        }else {
            println("No data")
        }
    }
}