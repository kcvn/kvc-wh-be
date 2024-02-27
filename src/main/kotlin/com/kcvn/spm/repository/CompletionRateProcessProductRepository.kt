package com.kcvn.spm.repository

import com.kcvn.spm.app.completionrate.payload.request.CompletionRateProcessProductRequest
import com.kcvn.spm.app.completionrate.payload.response.CompletionRateProcessProductResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProcessProduct
import com.kcvn.spm.model.tables.references.*
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class CompletionRateProcessProductRepository(private val context: DSLContext) : SortingRepository() {

    fun update(data: CompletionRateProcessProduct): CompletionRateProcessProduct? {
        return context
            .update(COMPLETION_RATE_PROCESS_PRODUCT)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.PRODUCT_NAME_SHORTCUT, data.productNameShortcut)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.RATE, data.rate)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.LAYER_CODE, data.layerCode)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .set(COMPLETION_RATE_PROCESS_PRODUCT.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.EXPIRATION_DATE, data.expirationDate)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.EFFECTIVE_DATE, data.effectiveDate)
            .set(COMPLETION_RATE_PROCESS_PRODUCT.PROCESS_CODE, data.processCode)
            .where(COMPLETION_RATE_PROCESS_PRODUCT.ID.eq(data.id))
            .returningResult(COMPLETION_RATE_PROCESS_PRODUCT)
            .fetchInto(CompletionRateProcessProduct::class.java).firstOrNull()
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
        search: CompletionRateProcessProductRequest?,
        pageable: Pageable?
    ): Pair<List<CompletionRateProcessProductResponse>, Int> {
        var condition: Condition = DSL.noCondition()

        if (search != null) {
            if (search.productNameShortCut != null) {
                val lowerProductNameShortCutSearch = DSL.lower(search.productNameShortCut)
                condition = condition.and(DSL.lower(COMPLETION_RATE_PROCESS_PRODUCT.PRODUCT_NAME_SHORTCUT).contains(lowerProductNameShortCutSearch))
            }

            if (search.processCode != null) {
                val lowerProcessCodeSearch = DSL.lower(search.processCode)
                condition = condition.and(DSL.lower(COMPLETION_RATE_PROCESS_PRODUCT.PROCESS_CODE).contains(lowerProcessCodeSearch))
            }
        }

        val maxEffectiveDatesMap = mutableMapOf<String, OffsetDateTime?>()

        val completionRateProcessesQuery = context.select(
            COMPLETION_RATE_PROCESS_PRODUCT.ID,
            COMPLETION_RATE_PROCESS_PRODUCT.KEY,
            COMPLETION_RATE_PROCESS_PRODUCT.PROCESS_CODE,
            COMPLETION_RATE_PROCESS_PRODUCT.LAYER_CODE,
            COMPLETION_RATE_PROCESS_PRODUCT.RATE,
            COMPLETION_RATE_PROCESS_PRODUCT.PRODUCT_NAME_SHORTCUT,
            COMPLETION_RATE_PROCESS_PRODUCT.EFFECTIVE_DATE,
            PROCESS_MASTER.PROCESS_NAME,
            PROCESS_MASTER.PROCESS_NAME_JP
        )
            .from(
                COMPLETION_RATE_PROCESS_PRODUCT
                    .join(PROCESS_MASTER).on(COMPLETION_RATE_PROCESS_PRODUCT.PROCESS_CODE.eq(PROCESS_MASTER.PROCESS_CODE))
                    .where(PROCESS_MASTER.IS_DELETED.eq(false))
            )
            .where(condition.and(COMPLETION_RATE_PROCESS_PRODUCT.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable?.sort, COMPLETION_RATE_PROCESS_PRODUCT.UPDATED_DATE))
            .limit(pageable?.pageSize ?: 10)
            .offset(pageable?.offset ?: 0)
            .fetchInto(CompletionRateProcessProductResponse::class.java)

        completionRateProcessesQuery.forEach { product ->
            val currentMaxEffectiveDate = maxEffectiveDatesMap[product.key]
            if (currentMaxEffectiveDate == null || (product.effectiveDate != null && product.effectiveDate > currentMaxEffectiveDate)) {
                maxEffectiveDatesMap[product.key!!] = product.effectiveDate
            }
        }

        val filteredList = completionRateProcessesQuery.filter { product ->
            val maxEffectiveDate = maxEffectiveDatesMap[product.key]
            product.effectiveDate != null && product.effectiveDate == maxEffectiveDate
        }

        val total = context.fetchCount(COMPLETION_RATE_PROCESS_PRODUCT, condition)

        return Pair(filteredList, total)
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



    fun add(data: CompletionRateProcessProduct) : CompletionRateProcessProduct? {
        return try {
            context
                .insertInto(
                    COMPLETION_RATE_PROCESS_PRODUCT,
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

                    data.key,
                    data.productNameShortcut,
                    data.processCode,
                    data.layerCode,
                    data.rate,
                    data.createdDate ?: OffsetDateTime.now(),
                    data.createdBy ?: "SYSTEM",
                    data.isDeleted ?: false,
                    data.updatedDate ?: OffsetDateTime.now(),
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


    fun getCompletionRateProcessProductWithMaxEffectivedateByName(name: String): CompletionRateProcessProduct? {

        val record = context.selectFrom(COMPLETION_RATE_PROCESS_PRODUCT)
            .where(
                COMPLETION_RATE_PROCESS_PRODUCT.IS_DELETED.eq(false)
                    .and(COMPLETION_RATE_PROCESS_PRODUCT.KEY.eq(name))
            )
            .orderBy(COMPLETION_RATE_PROCESS_PRODUCT.EFFECTIVE_DATE.desc())
            .limit(1)
            .fetchOne()
        return record?.into(CompletionRateProcessProduct::class.java)

    }

}