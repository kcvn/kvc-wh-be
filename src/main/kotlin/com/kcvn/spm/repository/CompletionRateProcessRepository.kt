package com.kcvn.spm.repository

import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessProductResponse
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessResponse
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProcess
import com.kcvn.spm.model.tables.references.COMPLETION_RATE_PROCESS
import com.kcvn.spm.model.tables.references.PROCESS_PROCEDURE_STRUCTURE
import com.kcvn.spm.model.tables.references.PRODUCT_PROCESS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class CompletionRateProcessRepository(private val context: DSLContext) : SortingRepository() {


    fun findByKeywordPaginated(keyword: String?, pageable: Pageable): Pair<List<CompletionRateProcessResponse>, Int?>
    {
        var condition: Condition = DSL.noCondition()
        if(keyword != null){
            val lowerKeyword = DSL.lower(keyword);
            condition = condition.and(DSL.lower(COMPLETION_RATE_PROCESS.KEY).contains(lowerKeyword))
        }

        val completionRateProcessQuery = context
            .select(
                COMPLETION_RATE_PROCESS.ID,
                COMPLETION_RATE_PROCESS.KEY,
                COMPLETION_RATE_PROCESS.RATE,
            )
            .from(COMPLETION_RATE_PROCESS)
            .where(condition.and(COMPLETION_RATE_PROCESS.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, COMPLETION_RATE_PROCESS.PROCESS_CODE))
            .limit(pageable.pageSize)
            .offset(pageable.offset)
            .fetchInto(CompletionRateProcessResponse::class.java)
        val queryTotal =  context
            .selectCount()
            .from(COMPLETION_RATE_PROCESS)
            .where(condition.and(COMPLETION_RATE_PROCESS.IS_DELETED.eq(false)))
        val totalCount = context.fetchOne(queryTotal)?.value1()
        return  Pair(completionRateProcessQuery, totalCount);
    }


    fun getListCompletionRateProcessByKey(productNames: List<String>): List<CompletionRateProcess> {
        return context.selectFrom(COMPLETION_RATE_PROCESS)
            .where(
                COMPLETION_RATE_PROCESS.KEY.`in`(productNames)
                    .and(COMPLETION_RATE_PROCESS.IS_DELETED.eq(false))
            )
            .fetchInto(CompletionRateProcess::class.java)
    }

    fun getPaginatedCompletionRateProcesses(
        search: String?,
        pageable: Pageable?
    ): Pair<List<CompletionRateProcessProductResponse>, Int> {
        var condition: Condition = DSL.noCondition()

        if (search != null) {
            val lowerSearch = DSL.lower(search)
            val searchCondition = DSL.lower(COMPLETION_RATE_PROCESS.PROCESS_CODE).contains(lowerSearch)
                .or(DSL.lower(PRODUCT_PROCESS.PROCESS_NAME).contains(lowerSearch))
                .or(DSL.lower(PRODUCT_PROCESS.PROCESS_NAME_JP).contains(lowerSearch))
            condition = condition.and(searchCondition)


        }

        val completionRateProcessesQuery = context.select(
            COMPLETION_RATE_PROCESS.ID,
            COMPLETION_RATE_PROCESS.KEY,
            COMPLETION_RATE_PROCESS.PROCESS_CODE,
            COMPLETION_RATE_PROCESS.LAYER_CODE,
            COMPLETION_RATE_PROCESS.RATE,
            PRODUCT_PROCESS.PROCESS_NAME,
            PRODUCT_PROCESS.PROCESS_NAME_JP
        )
            .from(COMPLETION_RATE_PROCESS.join(PROCESS_PROCEDURE_STRUCTURE.join(PRODUCT_PROCESS)
                .on(PROCESS_PROCEDURE_STRUCTURE.ID.eq(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID)).and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
                .on(COMPLETION_RATE_PROCESS.PROCESS_CODE.eq(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE)).and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false)))
            .where(condition.and(COMPLETION_RATE_PROCESS.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable?.sort, COMPLETION_RATE_PROCESS.UPDATED_DATE))
            .limit(pageable?.pageSize ?: 10)
            .offset(pageable?.offset ?: 0)
            .fetchInto(CompletionRateProcessProductResponse::class.java)

        val total = context.fetchCount(COMPLETION_RATE_PROCESS, COMPLETION_RATE_PROCESS.IS_DELETED.eq(false))

        return Pair(completionRateProcessesQuery, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        return when (sortFieldName) {
            "id" -> COMPLETION_RATE_PROCESS.ID
            "key" -> COMPLETION_RATE_PROCESS.KEY
            "processCode" -> COMPLETION_RATE_PROCESS.PROCESS_CODE
            // Add more cases for other fields as needed
            else -> throw IllegalArgumentException("Could not find table field: $sortFieldName")
        }
    }

    fun getAllProducts(): List<CompletionRateProcess> {
        return context.selectFrom(COMPLETION_RATE_PROCESS)
            .fetchInto(CompletionRateProcess::class.java)
    }

    fun findByObjectId(objectId: String): CompletionRateProcess? {
        return context.selectFrom(COMPLETION_RATE_PROCESS).where(COMPLETION_RATE_PROCESS.ID.eq(objectId))
            .fetchInto(CompletionRateProcess::class.java)
            .firstOrNull()
    }

    fun findByObjectId(objectIds: List<String>): List<CompletionRateProcess> {
        return context.selectFrom(COMPLETION_RATE_PROCESS)
            .where(COMPLETION_RATE_PROCESS.ID.`in`(objectIds))
            .fetchInto(CompletionRateProcess::class.java)
    }

    fun add(data: CompletionRateProcess): CompletionRateProcess? {
       return try {
            context
                .insertInto(
                    COMPLETION_RATE_PROCESS,
                    COMPLETION_RATE_PROCESS.KEY,
                    COMPLETION_RATE_PROCESS.PROCESS_CODE,
                    COMPLETION_RATE_PROCESS.LAYER_CODE,
                    COMPLETION_RATE_PROCESS.RATE,
                    COMPLETION_RATE_PROCESS.CREATED_DATE,
                    COMPLETION_RATE_PROCESS.CREATED_BY,
                    COMPLETION_RATE_PROCESS.IS_DELETED,
                    COMPLETION_RATE_PROCESS.UPDATED_DATE,
                    COMPLETION_RATE_PROCESS.EXPIRATION_DATE,
                    COMPLETION_RATE_PROCESS.EFFECTIVE_DATE
                )
                .values(

                     data.key,
                     data.processCode,
                     data.layerCode,
                     data.rate,
                     data.createdDate ?: OffsetDateTime.now(ZoneOffset.UTC),
                     data.createdBy ?: "admin",
                     data.isDeleted ?: false,
                     data.updatedDate ?: OffsetDateTime.now(ZoneOffset.UTC),
                     data.expirationDate,
                     data.effectiveDate
                )
                .returningResult(COMPLETION_RATE_PROCESS)
                .fetchOne()
                ?.into(CompletionRateProcess::class.java)
        } catch (e: Exception) {
           null
        }
    }


    fun delete(id: String) {
        context.update(COMPLETION_RATE_PROCESS)
            .set(COMPLETION_RATE_PROCESS.IS_DELETED, true)
            .where(COMPLETION_RATE_PROCESS.ID.eq(id))
            .execute()
    }


    fun update(data: CompletionRateProcess): CompletionRateProcess? {
        return context
            .update(COMPLETION_RATE_PROCESS)
            .set(COMPLETION_RATE_PROCESS.RATE, data.rate)
            .set(COMPLETION_RATE_PROCESS.CREATED_DATE, data.createdDate)
            .set(COMPLETION_RATE_PROCESS.LAYER_CODE, data.layerCode)
            .set(COMPLETION_RATE_PROCESS.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .set(COMPLETION_RATE_PROCESS.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
            .set(COMPLETION_RATE_PROCESS.IS_DELETED, data.isDeleted)
            .set(COMPLETION_RATE_PROCESS.EXPIRATION_DATE, data.expirationDate)
            .set(COMPLETION_RATE_PROCESS.EFFECTIVE_DATE, data.effectiveDate)
            .set(COMPLETION_RATE_PROCESS.PROCESS_CODE, data.processCode)
            .set(COMPLETION_RATE_PROCESS.KEY, data.key)
            .where(COMPLETION_RATE_PROCESS.ID.eq(data.id)) // Assuming ID is the primary key
            .returningResult(COMPLETION_RATE_PROCESS)
            .fetchOne()
            ?.into(CompletionRateProcess::class.java)
    }


}
