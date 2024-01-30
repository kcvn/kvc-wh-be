package com.kcvn.spm.repository

import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.model.tables.references.AUTH_USER
import com.kcvn.spm.model.tables.references.PRODUCT_PROCESS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ProductProcessRepository(private val context: DSLContext) : SortingRepository()  {
    fun findByKeywordPaginated(keyword: String?,hasProcessConvertCode: Boolean, pageable: Pageable): Pair<List<ProductProcess>, Int>
    {
        var condition: Condition = DSL.noCondition()
        if(keyword != null){
            val lowerKeyword = DSL.lower(keyword);
            condition = condition.and(DSL.lower(PRODUCT_PROCESS.PROCESS_NAME).contains(lowerKeyword))
        }
        if(hasProcessConvertCode){
            condition = condition.and(PRODUCT_PROCESS.PROCESS_CONVERT_CODE.isNull
                .or(PRODUCT_PROCESS.PROCESS_STATISTIC_CODE.isNull))
        }
        val productProcessQuery = context.selectFrom(PRODUCT_PROCESS)
            .where(condition.and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, PRODUCT_PROCESS.CREATED_DATE))
            .limit(pageable.pageSize)
            .offset(pageable.offset)
            .fetchInto(ProductProcess::class.java)
        val total = context.fetchCount(PRODUCT_PROCESS, condition.and((PRODUCT_PROCESS.IS_DELETED.eq(false))));
        return  Pair(productProcessQuery, total);
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "productName" -> {
                PRODUCT_PROCESS.PRODUCT_NAME
            }
            "layerCode" -> {
                PRODUCT_PROCESS.LAYER_CODE
            }
            "processConvertCode" -> {
                PRODUCT_PROCESS.PROCESS_CONVERT_CODE
            }
            else -> {
                val errorMessage = java.lang.String.format("Could not find table field: $sortFieldName")
                throw InvalidDataAccessApiUsageException(errorMessage)
            }
        }
        return sortField
    }
}

