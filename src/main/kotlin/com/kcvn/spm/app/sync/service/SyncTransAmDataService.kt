package com.kcvn.spm.app.sync.service

import com.kcvn.spm.app.sync.payload.response.SyncProcessMasterResponse
import com.kcvn.spm.app.sync.payload.response.SyncProcessProcedureStructureResponse
import com.kcvn.spm.app.sync.payload.response.SyncWorkResultResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.GrpProcessCode
import com.kcvn.spm.common.constants.SyncType
import com.kcvn.spm.common.constants.TransAmTable
import com.kcvn.spm.common.constants.YesNoConfig
import com.kcvn.spm.common.exception.BusinessException
import com.kcvn.spm.common.helper.StringHelper
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.common.util.DSLContextExtension
import com.kcvn.spm.config.PropertiesConfig
import com.kcvn.spm.model.tables.pojos.AppSetting
import com.kcvn.spm.model.tables.pojos.ProcessMaster
import com.kcvn.spm.model.tables.pojos.ProcessProcedureStructure
import com.kcvn.spm.model.tables.pojos.SyncHistory
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.model.tables.references.APP_SETTING
import com.kcvn.spm.repository.ProcessMasterRepository
import com.kcvn.spm.repository.ProcessProcedureStructureRepository
import com.kcvn.spm.repository.ProductRepository
import com.kcvn.spm.repository.SyncHistoryRepository
import com.kcvn.spm.repository.SystemLockRepository
import com.kcvn.spm.repository.WorkResultRepository
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SQLDialect
import org.jooq.Table
import org.jooq.impl.DSL
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.format.DateTimeFormatter


@Service
@Transactional
class SyncTransAmDataService(
    propertiesConfig: PropertiesConfig,
    private val syncHistoryRep: SyncHistoryRepository,
    private val processProcedureStructureRep: ProcessProcedureStructureRepository,
    private val processMasterRep: ProcessMasterRepository,
    private val workResultRep: WorkResultRepository,
    private val context: DSLContext,
    private val productRep: ProductRepository,
    private val systemLockRep: SystemLockRepository
) {
    private val transAmDSLContext: DSLContext = DSLContextExtension.createDSLContext(
        propertiesConfig.tranAmDbUrl,
        propertiesConfig.tranAmDbUser,
        propertiesConfig.tranAmDbPassword,
        SQLDialect.DEFAULT
    )

    private val schema = propertiesConfig.tranAmDbSchema
    private val hasSchema = propertiesConfig.tranAmDbHasSchema == YesNoConfig.YES

    fun syncProcessProcedureStructure() {
        if (systemLockRep.isLock(Constants.SYSTEM_LOCK_PRODUCT_PROCESS))
            throw BusinessException(CommonUtils.getMessage("action.systemLock"))

        val syncHistory = syncHistoryRep.findByType(SyncType.PROCESS_PROCEDURE_STRUCTURE)
        val table: Table<*> = if (hasSchema) {
            DSL.table(DSL.name(schema, TransAmTable.PROCESS_PROCEDURE_STRUCTURE))
        } else {
            DSL.table(DSL.name(TransAmTable.PROCESS_PROCEDURE_STRUCTURE))
        }
        var condition: Condition = DSL.noCondition()
        if (syncHistory != null) {
            condition = condition.and(
                DSL.field(TransAmTable.TOROKU_DATE).gt(syncHistory.createdDate?.toLocalDateTime())
                    .or(DSL.field(TransAmTable.KOSHIN_DATE).gt(syncHistory.createdDate?.toLocalDateTime()))
            )
        }
        val processFlows = this.transAmDSLContext.select().from(table).where(condition)
            .fetchInto(SyncProcessProcedureStructureResponse::class.java)
        val objectIds = processFlows.mapNotNull { x -> x.OBJECT_ID }

        val processFlowDatas = processProcedureStructureRep.findByObjectId(objectIds)

        val lstInsert = mutableListOf<ProcessProcedureStructure>()
        val lstDelete = mutableListOf<ProcessProcedureStructure>()

        for (item in processFlows) {
            val exist = processFlowDatas.find { x -> x.objectId == item.OBJECT_ID }
            val dataProcess = createModelProcessProcedureStructure(item)
            if (exist != null) {
                lstDelete.add(exist)
            }
            lstInsert.add(dataProcess)
        }

        try {
            processProcedureStructureRep.removeRange(lstDelete)
            processProcedureStructureRep.addRange(lstInsert)
            insertSyncHistory(
                TransAmTable.PROCESS_PROCEDURE_STRUCTURE,
                SyncType.PROCESS_PROCEDURE_STRUCTURE,
                SyncType.PROCESS_PROCEDURE_STRUCTURE
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun syncProcessMaster() {
        if (systemLockRep.isLock(Constants.SYSTEM_LOCK_PRODUCT_PROCESS))
            throw BusinessException(CommonUtils.getMessage("action.systemLock"))

        val syncHistory = syncHistoryRep.findByType(SyncType.PROCESS_MASTER)
        val table: Table<*> = if (hasSchema) {
            DSL.table(DSL.name(schema, TransAmTable.PROCESS_MASTER))
        } else {
            DSL.table(DSL.name(TransAmTable.PROCESS_MASTER))
        }
        var condition: Condition = DSL.noCondition()
        if (syncHistory != null) {
            condition = condition.and(
                DSL.field(TransAmTable.TOROKU_DATE).gt(syncHistory.createdDate?.toLocalDateTime())
                    .or(DSL.field(TransAmTable.KOSHIN_DATE).gt(syncHistory.createdDate?.toLocalDateTime()))
            )
        }
        val processMaster = this.transAmDSLContext.select().from(table).where(condition)
            .fetchInto(SyncProcessMasterResponse::class.java)
        val objectIds = processMaster.mapNotNull { x -> x.OBJECT_ID }

        val processMasterDatas = processMasterRep.findByObjectId(objectIds)

        val lstInsert = mutableListOf<ProcessMaster>()
        val lstDelete = mutableListOf<ProcessMaster>()

        for (item in processMaster) {
            val exist = processMasterDatas.find { x -> x.objectId == item.OBJECT_ID }
            val data = createModelProcessMaster(item)
            if (exist != null) {
                lstDelete.add(exist)
            }
            lstInsert.add(data)
        }

        try {
            processMasterRep.removeRange(lstDelete)
            processMasterRep.addRange(lstInsert)
            insertSyncHistory(
                TransAmTable.PROCESS_MASTER,
                SyncType.PROCESS_MASTER,
                SyncType.PROCESS_MASTER
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }

    }

    fun syncWorkResult() {
        val syncHistory = syncHistoryRep.findByType(SyncType.WORK_RESULT)
        val table: Table<*> = if (hasSchema) {
            DSL.table(DSL.name(schema, TransAmTable.WORK_RESULT))
        } else {
            DSL.table(DSL.name(TransAmTable.WORK_RESULT))
        }
        var condition: Condition = DSL.noCondition()
        if (syncHistory != null) {
            condition = condition.and(
                DSL.field(TransAmTable.TOROKU_DATE).gt(syncHistory.createdDate?.toLocalDateTime())
                    .or(DSL.field(TransAmTable.KOSHIN_DATE).gt(syncHistory.createdDate?.toLocalDateTime()))
            )
        }
        // Define your datetime range
        val key = "DATE_SYNC_DATA_FROM_TRANS_AM"
        val setting = context.selectFrom(APP_SETTING).where(APP_SETTING.KEY.eq(key)).fetchAnyInto(AppSetting::class.java)
        val value = setting?.value
        val date = LocalDate.parse(value, DateTimeFormatter.ofPattern("yyyy-MM-dd"))
        val startDate = date.atStartOfDay()

        condition = condition.and(DSL.field(TransAmTable.TOROKU_DATE).greaterOrEqual(startDate))

        val workResult = this.transAmDSLContext.select().from(table).where(condition)
            .fetchInto(SyncWorkResultResponse::class.java)

        val objectIds = workResult.mapNotNull { x -> x.OBJECT_ID }
        val workResultDatas = workResultRep.findByObjectId(objectIds)

        val lstInsert = mutableListOf<WorkResult>()
        val lstDelete = mutableListOf<WorkResult>()
        val lstProduct = mutableListOf<String>()
        for (item in workResult) {
            val exist = workResultDatas.find { x -> x.objectId == item.OBJECT_ID }
            val data = createModelWorkResult(item)
            if(data.processGrp == GrpProcessCode.XERANH){
                data.itemName?.let { lstProduct.add(it) }
            }
            if (exist != null) {

                lstDelete.add(exist)
            }
            lstInsert.add(data)
        }
        if(lstProduct.isNotEmpty()){
            val product = productRep.getByName(lstProduct)
            for(lstInsertItem in lstInsert){
                if(lstInsertItem.processGrp == GrpProcessCode.XERANH){
                    val productItem = product.find { x -> x.name == lstInsertItem.itemName }
                    if(productItem != null){
                        lstInsertItem.totalTapeQuantity  = lstInsertItem.totalSheetQuantity?.times(productItem.shBlock!!)
                        lstInsertItem.goodTapeQuantity   = productItem.shBlock?.let {
                            lstInsertItem.goodSheetQuantity?.times(
                                it
                            )
                        }

                    }
                }
            }
        }
        try {
            workResultRep.removeRange(lstDelete)
            workResultRep.addRange(lstInsert)
            insertSyncHistory(
                TransAmTable.WORK_RESULT,
                SyncType.WORK_RESULT,
                SyncType.WORK_RESULT
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun createModelWorkResult(request: SyncWorkResultResponse): WorkResult {
        return WorkResult(
            objectId = request.OBJECT_ID,
            androidId = request.ANDROID_ID,
            description = request.BIKO,
            grpDepartments = request.BUMON_GRP,
            departmentCode = request.BUSHO_CD,
            departmentName = request.BUSHO_MEI,
            orderCode = request.SEIZO_ORDER_NO,
            code = request.KANRI_NO,
            customerCode = request.KYAKUSAKI_CD,
            itemCode = request.HINMOKU_CD,
            itemName = request.KC_HINMEI,
            layerCode = StringHelper.intToStringD2(request.SO_NO),
            processCode = request.KOTEI_CD,
            processGrp = request.KOTEI_GRP,
            processName = request.KOTEI_MEI,
            processType = request.KOTEI_SHUBETSU,
            equipmentCode = request.SHIGEN_CD,
            equipmentName = request.SHIGEN_MEI,
            tapeLotNo = request.TAPE_LOT_NO,
            completionType = request.CHAKKAN_KBN,
            seidenNo = request.DAIHYO_SEIDEN_NO,
            version = request.EDABAN,
            furimukouType = request.FURIMUKE_KBN,
            isExclusiveOrException = request.HAITA_FLG,
            excessFraction = request.HASU,
            direction = request.HOKO,
            itemQuantity = request.HON_SU,
            shipmentStatus = request.SHUKKA_LOT_NO,
            actualResultCode = request.JISSEKI_CD,
            actualResultDepartment = request.JISSEKI_KANRI_BUMON_GRP,
            summaryResultDate = request.JISSEKI_KEIJO_DATE,
            enterActualResultType = request.JISSEKI_NYURYOKU_KBN,
            actualResultType = request.JISSEKI_SHIKIBETSU,
            projectCheck_1 = request.JOKEN_CHECK_KOMOKU_1,
            projectCheck_2 = request.JOKEN_CHECK_KOMOKU_2,
            projectCheck_3 = request.JOKEN_CHECK_KOMOKU_3,
            companyCode = request.KAISHA_CD,
            workStartBy = request.KAISHI_SAGYOSHA,
            managerCode = request.KANRISHA_CD,
            conversionFactor = request.KANZAN_JOSU,
            lonQuantity = request.KAN_SU,
            furimukouQuantity = request.FURIMUKE_SU,
            errorItemQuantity = request.FURYO_SU,
            hifurimukouQuantity = request.HIFURIMUKE_SU,
            inventoryItemQuantity = request.HORYU_SU,
            goodItemQuantity = request.RYOHIN_SU,
            regenerativeItemQuantity = request.SAISEI_SU,
            totalItemQuantity = request.SHORI_SU,
            adjustmentItemQuantity = request.TYOSEI_SU,
            furimukouTapeQuantity = request.KIBAN_FURIMUKE_SU,
            errorTapeQuantity = request.KIBAN_FURYO_SU,
            hifurimukouTapeQuantity = request.KIBAN_HIFURIMUKE_SU,
            inventoryTapeQuantity = request.KIBAN_HORYU_SU,
            goodTapeQuantity = request.KIBAN_RYOHIN_SU,
            regenerativeTapeQuantity = request.KIBAN_SAISEI_SU,
            totalTapeQuantity = request.KIBAN_SHORI_SU,
            adjustmentTapeQuantity = request.KIBAN_TYOSEI_SU,
            furimukouSheetQuantity = request.SHEET_FURIMUKE_SU,
            errorSheetQuantity = request.SHEET_FURYO_SU,
            hifurimukouSheetQuantity = request.SHEET_HIFURIMUKE_SU,
            inventorySheetQuantity = request.SHEET_HORYU_SU,
            goodSheetQuantity = request.SHEET_RYOHIN_SU,
            regenerativeSheetQuantity = request.SHEET_SAISEI_SU,
            totalSheetQuantity = request.SHEET_SHORI_SU,
            adjustmentSheetQuantity = request.SHEET_TYOSEI_SU,
            shiftWork = request.KINMUTAI_SHIFT,
            inputUnit = request.NYURYOKU_TANI,
            workCode_1 = request.SAGYO_CD1,
            workCode_2 = request.SAGYO_CD2,
            workCode_3 = request.SAGYO_CD3,
            workDate = request.SAGYO_DATE,
            workTime = request.SAGYO_TIME,
            workStartDate = request.SAGYO_KAISHI_DATE,
            workStartTime = request.SAGYO_KAISHI_TIME,
            workEndTime = request.SAGYO_SHURYO_TIME,
            workEndDate = request.SAGYO_SYURYO_DATE,
            workPlaceCode = request.SAGYOBA_CD,
            workPlaceName = request.SAGYOBA_MEI,
            team = request.SAGYO_JISSHI_HAN,
            memoWork = request.SAGYO_MEMO,
            workType = request.SAGYOKBN_CD,
            workImplementBy = request.SAGYOSHA_CD,
            regenerativeCode = request.SAISEI_CD,
            regenerativeType = request.SAISEI_KBN,
            regenerativeName = request.SAISEI_MEI,
            regenerativeProcessCode = request.SAISEISAKI_KOTEI_CD,
            regenerativeProcessName = request.SAISEISAKI_KOTEI_MEI,
            madeIn = request.SEIZOSAKI_CD,
            sheetFlag = request.SHEET_FLAG,
            unfinishedQuantity = request.SHIKAKARI_NYURYOKU_SU,
            remediationDirectiveNumber = request.SHOCHISHIJI_NO,
            deliverLotNo = request.SHUKKA_LOT_NO,
            endDate = request.SHURYO_GAPPI,
            total = request.SO_KOSU,
            price = request.TANKA,
            specialItem = request.TOKKI_JIKOU,
            createdDate = request.TOROKU_DATE,
            createdBy = request.TOROKUSHA,
            updatedDate = request.KOSHIN_DATE,
            updatedBy = request.KOSHINSHA
        )
    }

    private fun createModelProcessProcedureStructure(request: SyncProcessProcedureStructureResponse): ProcessProcedureStructure {
        return ProcessProcedureStructure(
            objectId = request.OBJECT_ID,
            companyCode = request.KAISHA_CD,
            grpDepartments = request.BUMON_GRP,
            remediationDirectiveNumber = request.SHOCHISHIJI_NO,
            productCode = request.KOTEI_TEJUN_CD,
            layerCode = StringHelper.intToStringD2(request.SO_NO),
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

    private fun createModelProcessMaster(request: SyncProcessMasterResponse): ProcessMaster {
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