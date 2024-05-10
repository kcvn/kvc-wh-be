package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.ProcessMaster
import com.kcvn.spm.model.tables.pojos.ProcessMasterData
import com.kcvn.spm.model.tables.references.PROCESS_MASTER
import com.kcvn.spm.model.tables.references.PROCESS_MASTER_DATA
import org.jooq.DSLContext
import org.jooq.impl.DSL
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
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val record = transactionalContext.newRecord(PROCESS_MASTER, model)
            transactionalContext.insertInto(PROCESS_MASTER).set(record).execute()
        }
    }

    fun delete(id: String) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(PROCESS_MASTER).where(PROCESS_MASTER.ID.eq(id)).execute()
        }
    }

    fun getProcessMasterDataByCode(processCodes: List<String>): List<ProcessMasterData> {
        return context.selectFrom(PROCESS_MASTER_DATA)
            .where(PROCESS_MASTER_DATA.PROCESS_CODE.`in`(processCodes).and(PROCESS_MASTER_DATA.IS_DELETED.eq(false)))
            .fetchInto(ProcessMasterData::class.java)
    }

    fun getByProcessCode(processCodes: List<String>): List<ProcessMaster> {
        val data = context.selectFrom(PROCESS_MASTER)
            .where(PROCESS_MASTER.PROCESS_CODE.`in`(processCodes).and(PROCESS_MASTER.IS_DELETED.eq(false)))
            .fetchInto(ProcessMaster::class.java)

        return data.groupBy { it.processCode }.map { item ->
            val res = item.value.sortedByDescending { it.updatedDate }.first()
            res
        }
    }

    fun addRange(data: List<ProcessMaster>) {
        val dataChunks = data.chunked(100)
        for (chunkItem in dataChunks) {
            context.transaction { configuration ->
                val transactionalContext = DSL.using(configuration)
                val records = chunkItem.map { x -> transactionalContext.newRecord(PROCESS_MASTER, x) }
                val query = records.map { x -> transactionalContext.insertInto(PROCESS_MASTER).set(x) }
                transactionalContext.batch(query).execute()
            }
        }
    }

    fun removeRange(data: List<ProcessMaster>) {
        val dataChunks = data.chunked(100)
        for (chunkItem in dataChunks) {
            context.transaction { configuration ->
                val transactionalContext = DSL.using(configuration)
                val query = chunkItem.map { x ->
                    transactionalContext
                        .deleteFrom(PROCESS_MASTER)
                        .where(PROCESS_MASTER.ID.eq(x.id))
                }
                transactionalContext.batch(query).execute()
            }
        }
    }

}