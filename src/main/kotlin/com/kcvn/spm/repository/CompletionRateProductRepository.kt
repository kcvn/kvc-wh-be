package com.kcvn.spm.repository
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProduct
import com.kcvn.spm.model.tables.references.COMPLETION_RATE_PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.UUID

@Repository
class CompletionRateProductRepository(private val context: DSLContext) : SortingRepository() {
    fun getByProduct(productNames: List<String>): List<CompletionRateProduct> {
        return context.selectFrom(COMPLETION_RATE_PRODUCT)
            .where(
                COMPLETION_RATE_PRODUCT.PRODUCT_NAME.`in`(productNames)
                    .and(COMPLETION_RATE_PRODUCT.IS_DELETED.eq(false))
            )
            .fetchInto(CompletionRateProduct::class.java)
    }

    fun getPaginatedCompletionRateProduct(
        search: String?,
        pageable: Pageable?
    ): Pair<List<CompletionRateProduct>, Int> {
        var condition: Condition = DSL.noCondition()

        if (search != null) {
            val lowerSearch = DSL.lower(search)
            condition = condition.and(DSL.lower(COMPLETION_RATE_PRODUCT.PRODUCT_NAME).contains(lowerSearch))
        }
        val check = context.selectFrom(COMPLETION_RATE_PRODUCT)
            .fetchInto(CompletionRateProduct::class.java)

        val completionRateProcessesQuery = context.selectFrom(COMPLETION_RATE_PRODUCT)
            .where(condition)
            .orderBy(getSortFields(pageable?.sort, COMPLETION_RATE_PRODUCT.PRODUCT_NAME))
            .limit(pageable?.pageSize)
            .offset(pageable?.offset)
            .fetchInto(CompletionRateProduct::class.java)

        val total = context.fetchCount(COMPLETION_RATE_PRODUCT, condition)
        return Pair(completionRateProcessesQuery, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        return when (sortFieldName) {
            "id" -> COMPLETION_RATE_PRODUCT.ID
            "productname" -> COMPLETION_RATE_PRODUCT.PRODUCT_NAME
            "rate" -> COMPLETION_RATE_PRODUCT.RATE
            // Add more cases for other fields as needed
            else -> throw IllegalArgumentException("Could not find table field: $sortFieldName")
        }
    }

    fun delete(id: String) {
        context.update(COMPLETION_RATE_PRODUCT)
            .set(COMPLETION_RATE_PRODUCT.IS_DELETED, true)
            .where(COMPLETION_RATE_PRODUCT.ID.eq(id))
            .execute()
    }


    fun add(data: CompletionRateProduct): CompletionRateProduct? {
        return try {
            context
                .insertInto(
                    COMPLETION_RATE_PRODUCT,
                    COMPLETION_RATE_PRODUCT.ID,
                    COMPLETION_RATE_PRODUCT.PRODUCT_NAME,
                    COMPLETION_RATE_PRODUCT.RATE,
                    COMPLETION_RATE_PRODUCT.CREATED_DATE,
                    COMPLETION_RATE_PRODUCT.CREATED_BY,
                    COMPLETION_RATE_PRODUCT.IS_DELETED,
                    COMPLETION_RATE_PRODUCT.UPDATED_DATE,
                )
                .values(
//                    data.id ?: UUID.randomUUID().toString(),
                    data.id ?: data.productName,
                    data.productName,
                    data.rate,
                    data.createdDate ?: LocalDateTime.now(),
                    data.createdBy ?: "SYSTEM",
                    data.isDeleted ?: false,
                    data.updatedDate ?: LocalDateTime.now()
                )
                .returningResult(COMPLETION_RATE_PRODUCT)
                .fetchOne()
                ?.into(CompletionRateProduct::class.java)
        } catch (e: Exception) {
            // Handle the exception as needed
            null
        }
    }



    fun update(data: CompletionRateProduct): CompletionRateProduct? {
        return context
            .update(COMPLETION_RATE_PRODUCT)
            .set(COMPLETION_RATE_PRODUCT.PRODUCT_NAME, data.productName)
            .set(COMPLETION_RATE_PRODUCT.RATE, data.rate)
            .set(COMPLETION_RATE_PRODUCT.CREATED_DATE, data.createdDate)
            .set(COMPLETION_RATE_PRODUCT.UPDATED_DATE, LocalDateTime.now(ZoneOffset.UTC))
            .set(COMPLETION_RATE_PRODUCT.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
            .set(COMPLETION_RATE_PRODUCT.IS_DELETED, data.isDeleted)
            .where(COMPLETION_RATE_PRODUCT.ID.eq(data.id)) // Assuming ID is the primary key
            .returningResult(COMPLETION_RATE_PRODUCT)
            .fetchOne()
            ?.into(CompletionRateProduct::class.java)
    }


}