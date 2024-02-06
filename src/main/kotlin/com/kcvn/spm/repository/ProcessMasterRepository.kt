package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.ProcessMaster
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
}