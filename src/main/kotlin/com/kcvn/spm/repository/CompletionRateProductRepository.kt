package com.kcvn.spm.repository
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.CompletionRateProduct
import com.kcvn.spm.model.tables.references.COMPLETION_RATE_PROCESS
import com.kcvn.spm.model.tables.references.COMPLETION_RATE_PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset

@Repository
class CompletionRateProductRepository(private val context: DSLContext) : SortingRepository() {
    fun getByProduct(productNames: List<String>): List<CompletionRateProduct>? {
        return context.selectFrom(COMPLETION_RATE_PRODUCT)
            .where(
                COMPLETION_RATE_PRODUCT.PRODUCT_NAME.`in`(productNames)
                    .and(COMPLETION_RATE_PRODUCT.IS_DELETED.eq(false))
            )
            .fetchInto(CompletionRateProduct::class.java)
    }

    fun getEffectiveByProduct(productNames: List<String>): List<CompletionRateProduct>? {
        return context.selectFrom(COMPLETION_RATE_PRODUCT)
            .where(
                COMPLETION_RATE_PRODUCT.PRODUCT_NAME.`in`(productNames)
                    .and(COMPLETION_RATE_PRODUCT.EXPIRATION_DATE.isNull)
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

        val crpSubquery = context.select(
            COMPLETION_RATE_PRODUCT.PRODUCT_NAME,
            DSL.max(COMPLETION_RATE_PRODUCT.EFFECTIVE_DATE).`as`("max_date")
        )
            .from(COMPLETION_RATE_PRODUCT)
            .where(condition.and(COMPLETION_RATE_PRODUCT.IS_DELETED.eq(false)))
            .groupBy(COMPLETION_RATE_PRODUCT.PRODUCT_NAME)

        val completionRateProcessesQuery = context.select(
            COMPLETION_RATE_PRODUCT.ID,
            COMPLETION_RATE_PRODUCT.PRODUCT_NAME,
            COMPLETION_RATE_PRODUCT.RATE,
            COMPLETION_RATE_PRODUCT.EFFECTIVE_DATE
        )
            .from(COMPLETION_RATE_PRODUCT)
            .join(crpSubquery)
            .on(COMPLETION_RATE_PRODUCT.PRODUCT_NAME.eq(crpSubquery.field(COMPLETION_RATE_PRODUCT.PRODUCT_NAME))
                .and(COMPLETION_RATE_PRODUCT.EFFECTIVE_DATE.eq(crpSubquery.field("max_date", OffsetDateTime::class.java))))
            .where(condition.and(COMPLETION_RATE_PRODUCT.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable?.sort, COMPLETION_RATE_PRODUCT.PRODUCT_NAME))
            .limit(pageable?.pageSize ?: 10)
            .offset(pageable?.offset ?: 0)
            .fetchInto(CompletionRateProduct::class.java)

        val total = context.selectDistinct(COMPLETION_RATE_PRODUCT.PRODUCT_NAME)
            .from(COMPLETION_RATE_PRODUCT)
            .where(condition)
            .fetch()
            .size

        return Pair(completionRateProcessesQuery, total)
    }


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        return when (sortFieldName) {
            "id" -> COMPLETION_RATE_PRODUCT.ID
            "productname" -> COMPLETION_RATE_PRODUCT.PRODUCT_NAME
            "rate" -> COMPLETION_RATE_PRODUCT.RATE
            // Add more cases for other fields as needed
            else -> throw IllegalArgumentException(CommonUtils.getMessage("sort.error.columnNotFound"))
        }
    }

    fun delete(id: String) {
        context.update(COMPLETION_RATE_PRODUCT)
            .set(COMPLETION_RATE_PRODUCT.IS_DELETED, true)
            .where(COMPLETION_RATE_PRODUCT.ID.eq(id))
            .execute()
    }


    fun add(data: CompletionRateProduct): CompletionRateProduct? {
        return context
                .insertInto(
                    COMPLETION_RATE_PRODUCT,
                    COMPLETION_RATE_PRODUCT.PRODUCT_NAME,
                    COMPLETION_RATE_PRODUCT.RATE,
                    COMPLETION_RATE_PRODUCT.CREATED_DATE,
                    COMPLETION_RATE_PRODUCT.CREATED_BY,
                    COMPLETION_RATE_PRODUCT.IS_DELETED,
                    COMPLETION_RATE_PRODUCT.UPDATED_DATE,
                    COMPLETION_RATE_PRODUCT.EFFECTIVE_DATE,
                    COMPLETION_RATE_PRODUCT.EXPIRATION_DATE
                )
                .values(
                    data.productName,
                    data.rate,
                    data.createdDate ?: OffsetDateTime.now(),
                    data.createdBy ?: CommonUtils.loggedInUser(),
                    data.isDeleted ?: false,
                    data.updatedDate ?: OffsetDateTime.now(),
                    data.effectiveDate,
                    data.expirationDate
                )
                .returningResult(COMPLETION_RATE_PRODUCT)
                .fetchOne()
                ?.into(CompletionRateProduct::class.java)
    }



    fun update(data: CompletionRateProduct): CompletionRateProduct? {
        return context
            .update(COMPLETION_RATE_PRODUCT)
            .set(COMPLETION_RATE_PRODUCT.PRODUCT_NAME, data.productName)
            .set(COMPLETION_RATE_PRODUCT.RATE, data.rate)
            .set(COMPLETION_RATE_PRODUCT.CREATED_DATE, data.createdDate)
            .set(COMPLETION_RATE_PRODUCT.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
            .set(COMPLETION_RATE_PRODUCT.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .set(COMPLETION_RATE_PRODUCT.IS_DELETED, data.isDeleted)
            .set(COMPLETION_RATE_PRODUCT.EFFECTIVE_DATE, data.effectiveDate)
            .set(COMPLETION_RATE_PRODUCT.EXPIRATION_DATE, data.expirationDate)
            .where(COMPLETION_RATE_PRODUCT.ID.eq(data.id)) // Assuming ID is the primary key
            .returningResult(COMPLETION_RATE_PRODUCT)
            .fetchOne()
            ?.into(CompletionRateProduct::class.java)
    }

    fun getCompletionRateProductWithMaxEffectivedateByName(name: String): CompletionRateProduct? {

        val record = context.selectFrom(COMPLETION_RATE_PRODUCT)
            .where(
                COMPLETION_RATE_PRODUCT.IS_DELETED.eq(false)
                    .and(COMPLETION_RATE_PRODUCT.PRODUCT_NAME.eq(name))
            )
            .orderBy(COMPLETION_RATE_PRODUCT.EFFECTIVE_DATE.desc())
            .limit(1)
            .fetchOne()
        return record?.into(CompletionRateProduct::class.java)

    }

}