package com.kcvn.spm.repository

import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.CompletionRateProduct
import com.kcvn.spm.model.tables.references.COMPLETION_RATE_PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class CompletionRateProductRepository(private val context: DSLContext) : SortingRepository(){
    fun getByProduct(productNames: List<String>) : List<CompletionRateProduct> {
        return context.selectFrom(COMPLETION_RATE_PRODUCT)
            .where(COMPLETION_RATE_PRODUCT.PRODUCT_NAME.`in`(productNames).and(COMPLETION_RATE_PRODUCT.IS_DELETED.eq(false)))
            .fetchInto(CompletionRateProduct::class.java)
    }
    fun getPaginatedCompletionRateProduct(search: String?, pageable: Pageable?): Pair<List<CompletionRateProduct>, Int> {
        var condition: Condition = DSL.noCondition()

        if (search != null) {
            val lowerSearch = DSL.lower(search)
            condition = condition.and(DSL.lower(COMPLETION_RATE_PRODUCT.PRODUCT_NAME).contains(lowerSearch))
        }
        val check = context.selectFrom(COMPLETION_RATE_PRODUCT)
            .fetchInto(CompletionRateProduct::class.java)

        val completionRateProcessesQuery = context.selectFrom(COMPLETION_RATE_PRODUCT)
            .where(condition)
            .orderBy(getSortFields(pageable?.sort, COMPLETION_RATE_PRODUCT.CREATED_DATE))
            .limit(pageable?.pageSize)
            .offset(pageable?.offset)
            .fetchInto(CompletionRateProduct::class.java)

        val total = context.fetchCount(COMPLETION_RATE_PRODUCT, condition)

        return Pair(completionRateProcessesQuery, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        return when (sortFieldName) {
            "id" -> COMPLETION_RATE_PRODUCT.ID

            "rate" -> COMPLETION_RATE_PRODUCT.RATE
            // Add more cases for other fields as needed
            else -> throw IllegalArgumentException("Could not find table field: $sortFieldName")
        }
    }

}