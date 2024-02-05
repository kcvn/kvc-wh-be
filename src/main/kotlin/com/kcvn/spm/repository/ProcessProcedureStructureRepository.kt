package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.ProcessProcedureStructure
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.model.tables.references.PROCESS_PROCEDURE_STRUCTURE
import com.kcvn.spm.model.tables.references.PRODUCT_PROCESS
import org.jooq.DSLContext
import org.springframework.stereotype.Repository

@Repository
class ProcessProcedureStructureRepository(private val context: DSLContext) {
    fun findByObjectId(objectId: Int): ProcessProcedureStructure? {
        return context.selectFrom(PROCESS_PROCEDURE_STRUCTURE).where(PROCESS_PROCEDURE_STRUCTURE.OBJECT_ID.eq(objectId))
            .fetchInto(ProcessProcedureStructure::class.java)
            .firstOrNull()
    }

    fun findByObjectId(objectIds: List<Int>): List<ProcessProcedureStructure> {
        return context.selectFrom(PROCESS_PROCEDURE_STRUCTURE)
            .where(PROCESS_PROCEDURE_STRUCTURE.OBJECT_ID.`in`(objectIds))
            .fetchInto(ProcessProcedureStructure::class.java)
    }

    fun add(model: ProcessProcedureStructure) {
        val record = context.newRecord(PROCESS_PROCEDURE_STRUCTURE, model)
        context.insertInto(PROCESS_PROCEDURE_STRUCTURE).set(record).execute()
    }

    fun delete(id: String) {
        context.deleteFrom(PROCESS_PROCEDURE_STRUCTURE).where(PROCESS_PROCEDURE_STRUCTURE.ID.eq(id)).execute()
    }

    fun getByProductName(productNames: List<String>) : List<ProcessProcedureStructure> {
        return context.selectFrom(PROCESS_PROCEDURE_STRUCTURE)
                .where(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.`in`(productNames).and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
                .fetchInto(ProcessProcedureStructure::class.java)
    }
}