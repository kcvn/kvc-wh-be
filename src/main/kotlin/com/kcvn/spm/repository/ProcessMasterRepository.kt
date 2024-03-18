package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.ProcessMaster
import com.kcvn.spm.model.tables.pojos.ProcessMasterData
import com.kcvn.spm.model.tables.references.PROCESS_MASTER
import com.kcvn.spm.model.tables.references.PROCESS_MASTER_DATA
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProcessMasterRepository(
    private val context: DSLContext
) {
    fun getListProcessCode(): List<String> {
        return context.select(PROCESS_MASTER.PROCESS_CODE)
            .from(PROCESS_MASTER)
            .where(PROCESS_MASTER.IS_DELETED.eq(false)).and(PROCESS_MASTER.IS_DELETED.eq(false))
            .fetchInto(String::class.java)
    }

    fun findByObjectId(objectIds: List<Long>): List<ProcessMaster> {
        return context.selectFrom(PROCESS_MASTER)
            .where(PROCESS_MASTER.OBJECT_ID.`in`(objectIds)).and(PROCESS_MASTER.IS_DELETED.eq(false))
            .fetchInto(ProcessMaster::class.java)
    }

    fun add(model: ProcessMaster) {
        val record = context.newRecord(PROCESS_MASTER, model)
        context.insertInto(PROCESS_MASTER).set(record).execute()
    }

    fun delete(id: String) {
        context.deleteFrom(PROCESS_MASTER).where(PROCESS_MASTER.ID.eq(id)).execute()
    }

    fun getProcessMasterDataByCode(processCodes: List<String>): List<ProcessMasterData> {
        return context.selectFrom(PROCESS_MASTER_DATA)
            .where(PROCESS_MASTER_DATA.PROCESS_CODE.`in`(processCodes).and(PROCESS_MASTER_DATA.IS_DELETED.eq(false)))
            .fetchInto(ProcessMasterData::class.java)
    }

}