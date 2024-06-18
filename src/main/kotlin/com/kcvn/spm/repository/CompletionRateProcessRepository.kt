package com.kcvn.spm.repository

import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessProductResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProcess
import com.kcvn.spm.model.tables.references.COMPLETION_RATE_PROCESS
import com.kcvn.spm.model.tables.references.PROCESS_MASTER
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class CompletionRateProcessRepository(private val context: DSLContext) : SortingRepository() {

    fun getListCompletionRateProcessByKey(productNames: List<String>): List<CompletionRateProcess>? {
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
            val lowerSearch = DSL.lower(search.trim())
            val searchCondition = DSL.lower(COMPLETION_RATE_PROCESS.PROCESS_CODE).containsIgnoreCase(lowerSearch)
                .or(DSL.lower(PROCESS_MASTER.PROCESS_NAME).containsIgnoreCase(lowerSearch))
                .or(DSL.lower(PROCESS_MASTER.PROCESS_NAME_JP).containsIgnoreCase(lowerSearch))
            condition = condition.and(searchCondition)
        }

        val crpSubquery = context.select(
            COMPLETION_RATE_PROCESS.KEY.`as`("key_map"),
            COMPLETION_RATE_PROCESS.PROCESS_CODE,
            DSL.max(COMPLETION_RATE_PROCESS.EFFECTIVE_DATE).`as`("max_date")
        )
            .from(COMPLETION_RATE_PROCESS)
            .groupBy(COMPLETION_RATE_PROCESS.KEY, COMPLETION_RATE_PROCESS.PROCESS_CODE)

        val completionRateProcessesQuery = context.select(
            COMPLETION_RATE_PROCESS.ID,
            COMPLETION_RATE_PROCESS.KEY,
            COMPLETION_RATE_PROCESS.PROCESS_CODE,
            COMPLETION_RATE_PROCESS.LAYER_CODE,
            COMPLETION_RATE_PROCESS.RATE,
            PROCESS_MASTER.PROCESS_NAME,
            PROCESS_MASTER.PROCESS_NAME_JP,
            COMPLETION_RATE_PROCESS.EFFECTIVE_DATE
        )
            .from(
                COMPLETION_RATE_PROCESS
                    .join(PROCESS_MASTER).on(COMPLETION_RATE_PROCESS.PROCESS_CODE.eq(PROCESS_MASTER.PROCESS_CODE))
                    .join(crpSubquery)
                    .on(COMPLETION_RATE_PROCESS.KEY.eq(crpSubquery.field("key_map", String::class.java))
                        .and(COMPLETION_RATE_PROCESS.EFFECTIVE_DATE.eq(crpSubquery.field("max_date", OffsetDateTime::class.java))))
                    .where(PROCESS_MASTER.IS_DELETED.eq(false)
                        .and(COMPLETION_RATE_PROCESS.IS_DELETED.eq(false)))
            )
            .where(condition.and(COMPLETION_RATE_PROCESS.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable?.sort, COMPLETION_RATE_PROCESS.UPDATED_DATE))
            .limit(pageable?.pageSize ?: 10)
            .offset(pageable?.offset ?: 0)
            .fetchInto(CompletionRateProcessProductResponse::class.java)


        val total = context.selectDistinct(COMPLETION_RATE_PROCESS.KEY)
            .from(
                COMPLETION_RATE_PROCESS
                    .join(PROCESS_MASTER)
                    .on(COMPLETION_RATE_PROCESS.PROCESS_CODE.eq(PROCESS_MASTER.PROCESS_CODE))
                    .where(condition)
            )
            .fetch()
            .size
        return Pair(completionRateProcessesQuery, total)
    }


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        return when (sortFieldName) {
            "id" -> COMPLETION_RATE_PROCESS.ID
            "key" -> COMPLETION_RATE_PROCESS.KEY
            "processCode" -> COMPLETION_RATE_PROCESS.PROCESS_CODE
            "layerCode" -> COMPLETION_RATE_PROCESS.LAYER_CODE
            else -> throw IllegalArgumentException(CommonUtils.getMessage("sort.error.columnNotFound"))
        }
    }

    fun getByProcessCode(processCodes: List<String>): List<CompletionRateProcess> {
        return context.selectFrom(COMPLETION_RATE_PROCESS)
            .where(COMPLETION_RATE_PROCESS.IS_DELETED.eq(false))
            .and(COMPLETION_RATE_PROCESS.PROCESS_CODE.`in`(processCodes))
            .and(COMPLETION_RATE_PROCESS.EXPIRATION_DATE.isNull)
            .fetchInto(CompletionRateProcess::class.java)
    }


    fun add(data: CompletionRateProcess): CompletionRateProcess? {
        var result: CompletionRateProcess? = null
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            result = try {
                transactionalContext
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
                        data.createdBy ?: CommonUtils.loggedInUser(),
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
        return result
    }

    fun delete(id: String) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.update(COMPLETION_RATE_PROCESS)
                .set(COMPLETION_RATE_PROCESS.IS_DELETED, true)
                .where(COMPLETION_RATE_PROCESS.ID.eq(id))
                .execute()
        }
    }

    fun update(data: CompletionRateProcess): CompletionRateProcess? {
        var result: CompletionRateProcess? = null
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            result = transactionalContext
                .update(COMPLETION_RATE_PROCESS)
                .set(COMPLETION_RATE_PROCESS.RATE, data.rate)
                .set(COMPLETION_RATE_PROCESS.CREATED_DATE, data.createdDate)
                .set(COMPLETION_RATE_PROCESS.LAYER_CODE, data.layerCode)
                .set(COMPLETION_RATE_PROCESS.UPDATED_DATE, Instant.now().atOffset(ZoneOffset.UTC))
                .set(COMPLETION_RATE_PROCESS.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
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
        return result
    }

    fun getCompletionRateProcessWithMaxEffectiveDateByName(name: String): CompletionRateProcess? {

        val record = context.selectFrom(COMPLETION_RATE_PROCESS)
            .where(
                COMPLETION_RATE_PROCESS.IS_DELETED.eq(false)
                    .and(COMPLETION_RATE_PROCESS.KEY.eq(name))
            )
            .orderBy(COMPLETION_RATE_PROCESS.EFFECTIVE_DATE.desc())
            .limit(1)
            .fetchOne()
        return record?.into(CompletionRateProcess::class.java)

    }

}
