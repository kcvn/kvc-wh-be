package com.kcvn.spm.repository
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.CompletionRateProcessProduct
import com.kcvn.spm.model.tables.references.COMPLETION_RATE_PROCESS_PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import org.springframework.data.domain.Pageable

@Repository
class CompletionRateProcessProductRepository (private val context: DSLContext) : SortingRepository(){
    fun getPaginatedCompletionRateProcessesProduct(search: String?, pageable: Pageable?): Pair<List<CompletionRateProcessProduct>, Int> {
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
            "process_code"-> COMPLETION_RATE_PROCESS_PRODUCT.PROCESS_CODE
            // Add more cases for other fields as needed
            else -> throw IllegalArgumentException("Could not find table field: $sortFieldName")
        }
    }

    fun getAllProducts(): List<CompletionRateProcessProduct> {
        return context.selectFrom(COMPLETION_RATE_PROCESS_PRODUCT)
            .fetchInto(CompletionRateProcessProduct::class.java)
    }
    fun findByObjectId(objectId: String): CompletionRateProcessProduct? {
        return context.selectFrom(COMPLETION_RATE_PROCESS_PRODUCT).where(COMPLETION_RATE_PROCESS_PRODUCT.ID.eq(objectId))
            .fetchInto(CompletionRateProcessProduct::class.java)
            .firstOrNull()
    }

    fun findByObjectId(objectIds: List<String>): List<CompletionRateProcessProduct> {
        return context.selectFrom(COMPLETION_RATE_PROCESS_PRODUCT)
            .where(COMPLETION_RATE_PROCESS_PRODUCT.ID.`in`(objectIds))
            .fetchInto(CompletionRateProcessProduct::class.java)
    }

    fun add(model: CompletionRateProcessProduct) {
        val record = context.newRecord(COMPLETION_RATE_PROCESS_PRODUCT, model)
        context.insertInto(COMPLETION_RATE_PROCESS_PRODUCT).set(record).execute()
    }

    fun delete(id: String) {
        context.deleteFrom(COMPLETION_RATE_PROCESS_PRODUCT).where(COMPLETION_RATE_PROCESS_PRODUCT.ID.eq(id)).execute()
    }
}