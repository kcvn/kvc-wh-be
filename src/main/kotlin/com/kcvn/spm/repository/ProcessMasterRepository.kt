package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.ProcessMaster
import com.kcvn.spm.model.tables.references.PROCESS_MASTER
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProcessMasterRepository (
    private val context: DSLContext
) {
    fun getListProcessMaster(): List<ProcessMaster> {
        return context.selectFrom(PROCESS_MASTER)
         .fetchInto(ProcessMaster::class.java)
    }

    fun findByObjectId(objectIds: List<Int>): List<ProcessMaster> {
        return context.selectFrom(PROCESS_MASTER)
            .where(PROCESS_MASTER.OBJECT_ID.`in`(objectIds))
            .fetchInto(ProcessMaster::class.java)
    }

    fun add(model: ProcessMaster) {
        val record = context.newRecord(PROCESS_MASTER, model)
        context.insertInto(PROCESS_MASTER).set(record).execute()
    }

    fun delete(id: String) {
        context.deleteFrom(PROCESS_MASTER).where(PROCESS_MASTER.ID.eq(id)).execute()
    }

}