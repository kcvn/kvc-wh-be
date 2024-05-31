package com.kcvn.spm.repository

import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.model.tables.pojos.ProcessProcedureStructure
import com.kcvn.spm.model.tables.references.PROCESS_PROCEDURE_STRUCTURE
import com.kcvn.spm.model.tables.references.PRODUCT
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class ProcessProcedureStructureRepository(private val context: DSLContext) {

    fun findByKey(keys: List<String>): List<ProcessProcedureStructure> {
        return context.selectFrom(PROCESS_PROCEDURE_STRUCTURE)
            .where(
                DSL.concat(
                    PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE,
                    PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE,
                    PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE
                ).`in`(keys)
            ).or(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.eq("000000"))
            .fetchInto(ProcessProcedureStructure::class.java)
    }

    fun getListProcessCode(): List<String> {
        return context.select(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE)
            .from(PROCESS_PROCEDURE_STRUCTURE)
            .where(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false))
            .fetchInto(String::class.java)
    }

    fun add(model: ProcessProcedureStructure) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val record = transactionalContext.newRecord(PROCESS_PROCEDURE_STRUCTURE, model)
            transactionalContext.insertInto(PROCESS_PROCEDURE_STRUCTURE).set(record).execute()
        }
    }

    fun delete(id: String) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(PROCESS_PROCEDURE_STRUCTURE).where(PROCESS_PROCEDURE_STRUCTURE.ID.eq(id)).execute()
        }
    }

    fun getByProductName(productNames: List<String?>): List<ProcessProcedureStructure> {
        return context.selectFrom(PROCESS_PROCEDURE_STRUCTURE)
            .where(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.`in`(productNames)
                .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false))
                .and(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.notEqual("0"))
                .and(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.notLike("0%")))
            .fetchInto(ProcessProcedureStructure::class.java)
    }

    fun getByFilterProcessStructureByInventoryProduct(request: ImportProcessRequest): ProcessProcedureStructure? {
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

    fun getByFilterProcessStructure(request: ImportProcessRequest): ProcessProcedureStructure? {
        val layerCodeInt = request.layerCode?.toInt()
        return context
            .selectFrom(PROCESS_PROCEDURE_STRUCTURE)
            .where(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.eq(request.productName)
                .and(PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE.cast(Int::class.java).eq(layerCodeInt))
                .and(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.eq(request.processCode))
                .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false)))
            .fetchAnyInto(ProcessProcedureStructure::class.java)

    }

    fun addRange(data: List<ProcessProcedureStructure>) {
        val dataChunks = data.chunked(100)
        for (chunkItem in dataChunks) {
            context.transaction { configuration ->
                val transactionalContext = DSL.using(configuration)
                val records = chunkItem.map { x -> transactionalContext.newRecord(PROCESS_PROCEDURE_STRUCTURE, x) }
                val query = records.map { x -> transactionalContext.insertInto(PROCESS_PROCEDURE_STRUCTURE).set(x) }
                transactionalContext.batch(query).execute()
            }
        }
    }

    fun removeRange(data: List<ProcessProcedureStructure>) {
        val dataChunks = data.chunked(100)
        for (chunkItem in dataChunks) {
            context.transaction { configuration ->
                val transactionalContext = DSL.using(configuration)
                val query = chunkItem.map { x ->
                    transactionalContext.update(PROCESS_PROCEDURE_STRUCTURE)
                        .set(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED, true)
                        .set(PROCESS_PROCEDURE_STRUCTURE.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                        .where(PROCESS_PROCEDURE_STRUCTURE.ID.eq(x.id))
                }
                transactionalContext.batch(query).execute()
            }
        }
    }
}