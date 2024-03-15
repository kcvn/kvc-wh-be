package com.kcvn.spm.repository

import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.model.tables.pojos.ProcessProcedureStructure
import com.kcvn.spm.model.tables.references.PROCESS_PROCEDURE_STRUCTURE
import com.kcvn.spm.model.tables.references.PRODUCT
import com.kcvn.spm.model.tables.references.PRODUCT_PROCESS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
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

    fun getListProcessCode(): List<String> {
        return context.select(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE)
            .from(PROCESS_PROCEDURE_STRUCTURE)
            .where(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false))
            .fetchInto(String::class.java)
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
                .where(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.`in`(productNames)
                    .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false))
                    .and(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.notEqual("0"))
                    .and(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.notLike("0%")))
                .fetchInto(ProcessProcedureStructure::class.java)
    }

    fun getByFilterProcessStructureByInventoryProduct(request: ImportProcessRequest) : ProcessProcedureStructure?{
        val layerCodeInt = request.layerCode?.toIntOrNull()
        return context.select(
            PROCESS_PROCEDURE_STRUCTURE.ID.`as`("id"),
        )
            .from(PRODUCT.join(PROCESS_PROCEDURE_STRUCTURE)
            .on(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.eq(PRODUCT.NAME)
                .and(PRODUCT.IS_DELETED.eq(false))))
            .where(PRODUCT.NAME.eq(request.productName)
                .and(PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE.cast(Int::class.java).eq(layerCodeInt))
                .and(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.eq(request.processCode))
                .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false)))
            .fetchAnyInto(ProcessProcedureStructure::class.java)

    }

    fun getByFilterProcessStructure(request: ImportProcessRequest) : ProcessProcedureStructure?{
        val layerCodeInt = request.layerCode?.toInt()
        return context
            .selectFrom(PROCESS_PROCEDURE_STRUCTURE)
            .where(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.eq(request.productName)
                .and(PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE.cast(Int::class.java).eq(layerCodeInt))
                .and(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.eq(request.processCode))
                .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false)))
            .fetchAnyInto(ProcessProcedureStructure::class.java)

    }
}