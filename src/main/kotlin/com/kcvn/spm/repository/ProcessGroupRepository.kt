package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.ProcessGroup
import com.kcvn.spm.model.tables.references.PROCESS_GROUP
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProcessGroupRepository (private val context: DSLContext) {

    fun getAll() : List<ProcessGroup> {
        return context.selectFrom(PROCESS_GROUP)
            .where(PROCESS_GROUP.IS_DELETED.eq(false))
            .fetchInto(ProcessGroup::class.java)
    }
}