package com.kcvn.spm.repository

import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.EquipmentProductivity
import com.kcvn.spm.model.tables.references.EQUIPMENT_PRODUCTIVITY
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class EquipmentProductivityRepository(private val context: DSLContext) {

    fun getEquipmentProductivity(): List<EquipmentProductivity> {
        return context.selectFrom(EQUIPMENT_PRODUCTIVITY)
            .where(EQUIPMENT_PRODUCTIVITY.IS_DELETED.eq(false))
            .fetchInto(EquipmentProductivity::class.java)
    }

    fun add(data: EquipmentProductivity): EquipmentProductivity? {
        var result: EquipmentProductivity? = null
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            result = transactionalContext.insertInto(
                EQUIPMENT_PRODUCTIVITY,
                EQUIPMENT_PRODUCTIVITY.EQUIPMENT_CODE,
                EQUIPMENT_PRODUCTIVITY.GRP_PROCESS,
                EQUIPMENT_PRODUCTIVITY.DESCRIPTION,
                EQUIPMENT_PRODUCTIVITY.FRAME_1,
                EQUIPMENT_PRODUCTIVITY.MOLD,
                EQUIPMENT_PRODUCTIVITY.OPERATING_RATE,
                EQUIPMENT_PRODUCTIVITY.TIME,
                EQUIPMENT_PRODUCTIVITY.COUNT,
                EQUIPMENT_PRODUCTIVITY.TASK,
                EQUIPMENT_PRODUCTIVITY.SHEET_HOUR_100,
                EQUIPMENT_PRODUCTIVITY.BLOCK_SH,
                EQUIPMENT_PRODUCTIVITY.CAP_BLOCK,
                EQUIPMENT_PRODUCTIVITY.CAP_HOUR,
                EQUIPMENT_PRODUCTIVITY.CAP_SHEET,
                EQUIPMENT_PRODUCTIVITY.CAP_SET,
                EQUIPMENT_PRODUCTIVITY.SLTB_BLOCK,
                EQUIPMENT_PRODUCTIVITY.SLTB_HOUR,
                EQUIPMENT_PRODUCTIVITY.SLTB_SHEET,
                EQUIPMENT_PRODUCTIVITY.SLTB_SET,
                EQUIPMENT_PRODUCTIVITY.QUANTITY_MACHINE,
                EQUIPMENT_PRODUCTIVITY.PROCESS_CODE,
                EQUIPMENT_PRODUCTIVITY.CREATED_DATE,
                EQUIPMENT_PRODUCTIVITY.CREATED_BY,
                EQUIPMENT_PRODUCTIVITY.IS_DELETED,
                EQUIPMENT_PRODUCTIVITY.UPDATED_DATE
            ).values(
                data.equipmentCode,
                data.grpProcess,
                data.description,
                data.frame_1,
                data.mold,
                data.operatingRate,
                data.time,
                data.count,
                data.task,
                data.sheetHour_100,
                data.blockSh,
                data.capBlock,
                data.capHour,
                data.capSheet,
                data.capSet,
                data.sltbBlock,
                data.sltbHour,
                data.sltbSheet,
                data.sltbSet,
                data.quantityMachine,
                data.processCode,
                data.createdDate ?: OffsetDateTime.now(),
                data.createdBy ?: CommonUtils.loggedInUser(),
                data.isDeleted ?: false,
                data.updatedDate ?: OffsetDateTime.now(),
            ).returningResult(EQUIPMENT_PRODUCTIVITY).fetchOne()?.into(EquipmentProductivity::class.java)
        }
        return result
    }

    fun getByProcessGroup(processGroupCodes: List<String>): List<EquipmentProductivity> {
        return context.selectFrom(EQUIPMENT_PRODUCTIVITY)
            .where(EQUIPMENT_PRODUCTIVITY.IS_DELETED.eq(false).and(EQUIPMENT_PRODUCTIVITY.GRP_PROCESS.`in`(processGroupCodes)))
            .fetchInto(EquipmentProductivity::class.java)
    }
}