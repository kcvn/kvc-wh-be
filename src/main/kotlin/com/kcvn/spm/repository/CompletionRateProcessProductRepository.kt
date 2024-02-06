package com.kcvn.spm.repository

import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProcessProduct
import com.kcvn.spm.model.tables.references.COMPLETION_RATE_PROCESS_PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset

@Repository
class CompletionRateProcessProductRepository(private val context: DSLContext) : SortingRepository() {
    fun update(data: CompletionRateProcessProduct): CompletionRateProcessProduct? {
        return context
            .update(COMPLETION_RATE_PROCESS_PRODUCT)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.PRODUCT_NAME_SHORTCUT, data.productNameShortcut)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.RATE, data.rate)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.CREATED_DATE, data.createdDate)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.LAYER_CODE, data.layerCode)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.UPDATED_DATE, LocalDateTime.now(ZoneOffset.UTC))
            .set(COMPLETION_RATE_PROCESS_PRODUCT.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
            .set(COMPLETION_RATE_PROCESS_PRODUCT.IS_DELETED, data.isDeleted)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.EXPIRATION_DATE, data.expirationDate)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.EFFECTIVE_DATE, data.effectiveDate)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.PROCESS_CODE, data.processCode)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.KEY, data.key)
            .where(COMPLETION_RATE_PROCESS_PRODUCT.ID.eq(data.id)) // Assuming ID is the primary key
            .returningResult(COMPLETION_RATE_PROCESS_PRODUCT)
            .fetchOne()
            ?.into(CompletionRateProcessProduct::class.java)
    }

    fun getListProductByKey(productKeys: List<String>): List<CompletionRateProcessProduct> {
        return context.selectFrom(COMPLETION_RATE_PROCESS_PRODUCT)
            .where(
                COMPLETION_RATE_PROCESS_PRODUCT.KEY.`in`(productKeys)
                    .and(COMPLETION_RATE_PROCESS_PRODUCT.IS_DELETED.eq(false))
            )
            .fetchInto(CompletionRateProcessProduct::class.java)
    }


    fun getPaginatedCompletionRateProcessesProduct(
        search: String?,
        pageable: Pageable?
    ): Pair<List<CompletionRateProcessProduct>, Int> {
        var condition: Condition = DSL.noCondition()

        if (search != null) {
            val lowerSearch = DSL.lower(search)
            condition = condition.and(DSL.lower(COMPLETION_RATE_PROCESS_PRODUCT.KEY).contains(lowerSearch))
        }

        val completionRateProcessesQuery = context.selectFrom(COMPLETION_RATE_PROCESS_PRODUCT)
            .where(condition)
            .orderBy(getSortFields(pageable?.sort, COMPLETION_RATE_PROCESS_PRODUCT.UPDATED_DATE))
            .limit(pageable?.pageSize ?: 10)
            .offset(pageable?.offset ?: 0)
            .fetchInto(CompletionRateProcessProduct::class.java)

        val total = context.fetchCount(COMPLETION_RATE_PROCESS_PRODUCT, condition)

        return Pair(completionRateProcessesQuery, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        return when (sortFieldName) {
            "id" -> COMPLETION_RATE_PROCESS_PRODUCT.ID
            "key" -> COMPLETION_RATE_PROCESS_PRODUCT.KEY
            "product_name_shortcut" -> COMPLETION_RATE_PROCESS_PRODUCT.PRODUCT_NAME_SHORTCUT
            "layerCode" -> COMPLETION_RATE_PROCESS_PRODUCT.LAYER_CODE
            "process_code" -> COMPLETION_RATE_PROCESS_PRODUCT.PROCESS_CODE
            // Add more cases for other fields as needed
            else -> throw IllegalArgumentException("Could not find table field: $sortFieldName")
        }
    }

    fun getAllProducts(): List<CompletionRateProcessProduct> {
        return context.selectFrom(COMPLETION_RATE_PROCESS_PRODUCT)
            .fetchInto(CompletionRateProcessProduct::class.java)
    }

    fun findByObjectId(objectId: String): CompletionRateProcessProduct? {
        return context.selectFrom(COMPLETION_RATE_PROCESS_PRODUCT)
            .where(COMPLETION_RATE_PROCESS_PRODUCT.ID.eq(objectId))
            .fetchInto(CompletionRateProcessProduct::class.java)
            .firstOrNull()
    }

    fun findByObjectId(objectIds: List<String>): List<CompletionRateProcessProduct> {
        return context.selectFrom(COMPLETION_RATE_PROCESS_PRODUCT)
            .where(COMPLETION_RATE_PROCESS_PRODUCT.ID.`in`(objectIds))
            .fetchInto(CompletionRateProcessProduct::class.java)
    }

    fun add(data: CompletionRateProcessProduct) : CompletionRateProcessProduct? {
        return try {
            context
                .insertInto(
                    COMPLETION_RATE_PROCESS_PRODUCT,
                    COMPLETION_RATE_PROCESS_PRODUCT.ID,
                    COMPLETION_RATE_PROCESS_PRODUCT.KEY,
                    COMPLETION_RATE_PROCESS_PRODUCT.PRODUCT_NAME_SHORTCUT,
                    COMPLETION_RATE_PROCESS_PRODUCT.PROCESS_CODE,
                    COMPLETION_RATE_PROCESS_PRODUCT.LAYER_CODE,
                    COMPLETION_RATE_PROCESS_PRODUCT.RATE,
                    COMPLETION_RATE_PROCESS_PRODUCT.CREATED_DATE,
                    COMPLETION_RATE_PROCESS_PRODUCT.CREATED_BY,
                    COMPLETION_RATE_PROCESS_PRODUCT.IS_DELETED,
                    COMPLETION_RATE_PROCESS_PRODUCT.UPDATED_DATE,
                    COMPLETION_RATE_PROCESS_PRODUCT.EXPIRATION_DATE,
                    COMPLETION_RATE_PROCESS_PRODUCT.EFFECTIVE_DATE
                )
                .values(
//                    data.id ?: UUID.randomUUID().toString(),
                    data.id ?: data.key,
                    data.key,
                    data.productNameShortcut,
                    data.processCode,
                    data.layerCode,
                    data.rate,
                    data.createdDate ?: LocalDateTime.now(),
                    data.createdBy ?: "SYSTEM",
                    data.isDeleted ?: false,
                    data.updatedDate ?: LocalDateTime.now(),
                    data.expirationDate ?: null,
                    data.effectiveDate
                )
                .returningResult(COMPLETION_RATE_PROCESS_PRODUCT)
                .fetchOne()
                ?.into(CompletionRateProcessProduct::class.java)
        } catch (e: Exception) {
            // Handle the exception as needed
            null
        }
    }

    fun delete(id: String) {
        context.update(COMPLETION_RATE_PROCESS_PRODUCT)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.IS_DELETED, true)
            .where(COMPLETION_RATE_PROCESS_PRODUCT.ID.eq(id))
            .execute()
    }
}