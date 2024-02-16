package com.kcvn.spm.app.sync.service

import com.kcvn.spm.app.sync.payload.response.SyncProcessMasterResponse
import com.kcvn.spm.app.sync.payload.response.SyncProcessProcedureStructureResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.TransAmTable
import com.kcvn.spm.common.util.DSLContextExtension
import com.kcvn.spm.config.PropertiesConfig
import com.kcvn.spm.model.tables.pojos.ProcessMaster
import com.kcvn.spm.model.tables.pojos.ProcessProcedureStructure
import com.kcvn.spm.model.tables.pojos.SyncHistory
import com.kcvn.spm.repository.ProcessMasterRepository
import com.kcvn.spm.repository.ProcessProcedureStructureRepository
import com.kcvn.spm.repository.SyncHistoryRepository
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.Table
import org.jooq.impl.DSL
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional


@Service
@Transactional
class SyncTransAmDataService(
    private val propertiesConfig: PropertiesConfig,
    private val syncHistoryRep: SyncHistoryRepository,
    private val processProcedureStructureRep: ProcessProcedureStructureRepository,
    private val processMasterRep: ProcessMasterRepository
) {
    private val transAmDSLContext: DSLContext = DSLContextExtension.createDSLContext(
        propertiesConfig.tranAmDbUrl,
        propertiesConfig.tranAmDbUser,
        propertiesConfig.tranAmDbPassword,
        SQLDialect.DEFAULT
    )

    fun syncProcessProcedureStructure() {
        val syncHistory = syncHistoryRep.findByType(Constants.PROCESS_PROCEDURE_STRUCTURE)
        val table: Table<*> = DSL.table(DSL.name(TransAmTable.PROCESS_PROCEDURE_STRUCTURE))
        var condition: Condition = DSL.noCondition()
        if (syncHistory != null) {
            condition = condition.and(
                DSL.field(TransAmTable.TOROKU_DATE).gt(syncHistory.createdDate)
                    .or(DSL.field(TransAmTable.KOSHIN_DATE).gt(syncHistory.createdDate))
            )
        }
        val processFlows = this.transAmDSLContext.select().from(table).where(condition)
            .fetchInto(SyncProcessProcedureStructureResponse::class.java)
        val objectIds = processFlows.mapNotNull { x -> x.OBJECT_ID }

        val processFlowDatas = processProcedureStructureRep.findByObjectId(objectIds)

        for (item in processFlows) {
            var exist = processFlowDatas.find { x -> x.objectId == item.OBJECT_ID }
            try {
                val dataProcess = createModelProcessProcedureStructure(item)
                if (exist != null) {
                    processProcedureStructureRep.delete(exist.id!!)
                }
                processProcedureStructureRep.add(dataProcess)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }

        insertSyncHistory(
            TransAmTable.PROCESS_PROCEDURE_STRUCTURE,
            Constants.PROCESS_PROCEDURE_STRUCTURE,
            Constants.PROCESS_PROCEDURE_STRUCTURE
        )
    }

    fun syncProcessMaster() {
        val syncHistory = syncHistoryRep.findByType(Constants.PROCESS_MASTER)
        val table: Table<*> = DSL.table(DSL.name(TransAmTable.PROCESS_MASTER))
        var condition: Condition = DSL.noCondition()
        if (syncHistory != null) {
            condition = condition.and(
                DSL.field(TransAmTable.TOROKU_DATE).gt(syncHistory.createdDate)
                    .or(DSL.field(TransAmTable.KOSHIN_DATE).gt(syncHistory.createdDate))
            )
        }
        val processMaster = this.transAmDSLContext.select().from(table).where(condition)
            .fetchInto(SyncProcessMasterResponse::class.java)
        val objectIds = processMaster.mapNotNull { x -> x.OBJECT_ID }

        val processMasterDatas = processMasterRep.findByObjectId(objectIds)

        for (item in processMaster) {
            var exist = processMasterDatas.find { x -> x.objectId == item.OBJECT_ID }
            try {
                val data = createModelProcessMaster(item)
                if (exist != null) {
                    processMasterRep.delete(exist.id!!)
                }
                processMasterRep.add(data)
            }
            catch (e: Exception) {
                e.printStackTrace()
            }
        }

        insertSyncHistory(
            TransAmTable.PROCESS_MASTER,
            Constants.PROCESS_MASTER,
            Constants.PROCESS_MASTER
        )
    }

    private fun createModelProcessProcedureStructure(request: SyncProcessProcedureStructureResponse): ProcessProcedureStructure {
        return ProcessProcedureStructure (
            objectId = request.OBJECT_ID,
            companyCode = request.KAISHA_CD,
            grpDepartments = request.BUMON_GRP,
            remediationDirectiveNumber = request.SHOCHISHIJI_NO,
            productCode = request.KOTEI_TEJUN_CD,
            layerCode = request.SO_NO,
            processCode = request.KOTEI_CD,
            processSequence = request.KOTEI_NO,
            processSequenceRev = request.KOTEI_TEJUN_REV,
            machiningSequenceRev = request.KAKOU_TEJUN_REV,
            completionRate = request.KANSEI_WARIAI,
            finishedProductRate = request.SETTEI_BUDOMARI,
            printType = request.KOTEI_INJI_KBN,
            processType = request.KOTEI_SHUBETSU,
            displayProcessSequence = request.KOTEI_HYOJI_JUN,
            inputSystem = request.NYURYOKU_TAIKEI,
            isActualResult = request.JISSEKI_NYURYOKU_UMU,
            shipmentSize = request.TONYU_LOT_SIZE,
            conversionFactor = request.KANZAN_JOSU,
            unitConversionFactor = request.KANZAN_JOSU_TANI,
            conversionFactorsErrorType = request.KANZAN_JOSU_FURYO_SHIYO_KBN,
            conversionFactorAdjustmentType = request.KANZAN_JOSU_CHOSEI_SHIYO_KBN,
            planLeadTimeNormally = request.TSUJO_LEAD_TIME_DANDORI,
            workLeadTimeNormally = request.TSUJO_LEAD_TIME_SAGYO,
            transportLeadTimeNormally = request.TSUJO_LEAD_TIME_UNPAN,
            planLeadTimeUrgently = request.TOKKYU_LEAD_TIME_DANDORI,
            workLeadTimeUrgently = request.TOKKYU_LEAD_TIME_SAGYO,
            transportLeadTimeUrgently = request.TOKKYU_LEAD_TIME_UNPAN,
            backlogType = request.YAMADUMI_KBN,
            unfinishedProductQuantity = request.SHIKAKARI_TORISU,
            standardProcedureType = request.SHIKAKARI_KOTEI_IDO_KBN,
            grpCheckProcess = request.TANAOROSI_KENSA_KOTEI_GRP,
            createdBy = request.TOROKUSHA,
            createdDate = request.TOROKU_DATE,
            updatedBy = request.KOSHINSHA,
            updatedDate = request.KOSHIN_DATE,
            isExclusiveOrException = request.HAITA_FLG,
            workplaceCode = request.SAGYOBA_CD,
        )
    }

    private fun createModelProcessMaster(request: SyncProcessMasterResponse) : ProcessMaster {
        return ProcessMaster(
            objectId = request.OBJECT_ID,
            companyCode = request.KAISHA_CD,
            grpDepartments = request.BUMON_GRP,
            processCode = request.KOTEI_CD,
            processName = request.KOTEI_MEI,
            processNameJp = request.KOTEI_MEI_JPN,
            grpProcess = request.KOTEI_GRP,
            grpProcessSummary = request.KOTEI_SHUKEI_GRP,
            productRateByMaterial = request.DEFAULT_BUDOMARI,
            completionRate = request.HYOJUN_KANSEI_WARIAI,
            inputSystem = request.HYOJUN_NYURYOKU_TAIKEI,
            isActualResult = request.HYOJUN_JISSEKI_NYURYOKU_UMU,
            officeCode = request.HYOJUN_JIGYOSHO_CD,
            shipmentSize = request.HYOJUN_TONYU_LOT_SIZE,
            conversionFactor = request.HYOJUN_KANZAN_JOSU,
            unitConversionFactor = request.HYOJUN_KANZAN_JOSU_TANI,
            conversionFactorsErrorType = request.HYOJUN_JOSU_FURYO_SIYO_KBN,
            conversionFactorAdjustmentType = request.HYOJUN_JOSU_CHOSEI_SIYO_KBN,
            planLeadTimeNormally = request.HYOJUN_TSUJO_LEAD_TIME_DANDORI,
            workLeadTimeNormally = request.HYOJUN_TSUJO_LEAD_TIME_SAGYO,
            transportLeadTimeNormally = request.HYOJUN_TSUJO_LEAD_TIME_UNPAN,
            planLeadTimeUrgently = request.HYOJUN_TOKYU_LEAD_TIME_DANDORI,
            workLeadTimeUrgently = request.HYOJUN_TOKYU_LEAD_TIME_SAGYO,
            transportLeadTimeUrgently = request.HYOJUN_TOKYU_LEAD_TIME_UNPAN,
            productRateByMaterialSetting = request.HYOJUN_SETTEI_BUDOMARI,
            backlogType = request.HYOJUN_YAMADUMI_KBN,
            unfinishedProductQuantity = request.HYOJUN_SHIKAKARI_TORISU,
            printType = request.HYOJUN_KOTEI_INJI_KBN,
            unfinishedProcedureType = request.HYOJUN_SHIKAKARI_KOTEI_IDO_KBN,
            standardProcedureType = request.HYOJUN_KOTEI_SYUBETSU,
            checkProcessCode = request.TNORSHKBN_CD,
            actualResultType = request.ZISSEKI_SHUKEI_KBN,
            leadTimeMin = request.LEAD_TIME_MIN,
            wsType = request.WS_TYPE,
            wsCode = request.WS_CD,
            persons = request.PERSONS,
            workTime = request.WORK_TIME,
            grpQuantity = request.GRP_SU,
            unit = request.UNIT,
            neckFlag = request.NECK_FLG,
            plannedProcessCode = request.KEIKAKU_KOTEI_CD,
            grpActualResult = request.JISSEKI_GRP,
            grpCapitalPrice = request.GENKA_GRP,
            isExclusiveOrException = request.HAITA_FLG,
            workplaceCode = request.SAGYOBA_CD,
            createdDate = request.TOROKU_DATE,
            createdBy = request.TOROKUSHA,
            updatedDate = request.KOSHIN_DATE,
            updatedBy = request.KOSHINSHA,
            isDeleted = false,
        )
    }
    private fun insertSyncHistory(source: String, destination: String, type: String) {
        val history = SyncHistory(
            source = source,
            destination = destination,
            type = type
        )

        syncHistoryRep.add(history)
    }
}