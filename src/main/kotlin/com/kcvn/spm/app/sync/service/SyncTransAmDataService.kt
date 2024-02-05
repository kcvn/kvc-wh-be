package com.kcvn.spm.app.sync.service

import com.kcvn.spm.app.sync.payload.response.SyncProcessProcedureStructureResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.TransAmTable
import com.kcvn.spm.common.util.DSLContextExtension
import com.kcvn.spm.config.PropertiesConfig
import com.kcvn.spm.model.tables.pojos.ProcessProcedureStructure
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
    private val processProcedureStructureRep: ProcessProcedureStructureRepository
) {
    private val transAmDSLContext: DSLContext = DSLContextExtension.createDSLContext(
        propertiesConfig.tranAmDbUrl,
        propertiesConfig.tranAmDbUser,
        propertiesConfig.tranAmDbPassword,
        SQLDialect.POSTGRES
    )

    fun syncProcessProcedureStructure() {
        val syncHistory = syncHistoryRep.findByType(Constants.SYNC_PROCESS_PROCEDURE_STRUCTURE)
        val table: Table<*> = DSL.table(DSL.name(TransAmTable.PROCESS_PROCEDURE_STRUCTURE))
        var condition: Condition = DSL.noCondition()
        if (syncHistory != null) {
            condition = condition.and(
                DSL.field(TransAmTable.FDMAM26_TOROKU_DATE).gt(syncHistory.createdDate)
                    .or(DSL.field(TransAmTable.FDMAM26_KOSHIN_DATE).gt(syncHistory.createdDate))
            )
        }
        val processFlows = this.transAmDSLContext.select().from(table).where(condition)
            .fetchInto(SyncProcessProcedureStructureResponse::class.java)
        val objectIds = processFlows.mapNotNull { x -> x.OBJECT_ID }

        val processFlowDatas = processProcedureStructureRep.findByObjectId(objectIds)

        for (item in processFlows) {
            var exist = processFlowDatas.find { x -> x.objectId == item.OBJECT_ID }
            try {
                val dataProcess = createModelProcess(item)
                if (exist != null) {
                    processProcedureStructureRep.delete(exist.id!!)
                }
                processProcedureStructureRep.add(dataProcess)
            }
            catch (ex: Exception) {

            }
        }
    }

    private fun createModelProcess(request: SyncProcessProcedureStructureResponse): ProcessProcedureStructure {
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
}