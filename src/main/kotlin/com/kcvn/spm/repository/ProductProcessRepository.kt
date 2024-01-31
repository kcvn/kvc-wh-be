package com.kcvn.spm.repository

import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.AuthRole
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.model.tables.references.AUTH_ROLE
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

    fun getByProduct(productNames: List<String>) : List<ProductProcess> {
        return context.selectFrom(PRODUCT_PROCESS)
            .where(PRODUCT_PROCESS.PRODUCT_NAME.`in`(productNames).and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
            .fetchInto(ProductProcess::class.java)
    }

    fun  getByProductProcessDetail(productName: String?) : List<ProductProcess?>? {
        val data = context.selectFrom(PRODUCT_PROCESS)
            .where((PRODUCT_PROCESS.PRODUCT_NAME.eq(productName)))
            .fetchInto(ProductProcess::class.java)
        return  data
    }

    fun  getByProductProcessDetailById(id: String?) : ProductProcess? {
        val data = context.selectFrom(PRODUCT_PROCESS)
            .where((PRODUCT_PROCESS.ID.eq(id)))
            .fetchAnyInto(ProductProcess::class.java)
        return  data
    }
    fun updateProductDetail(request: ProductProcess) :  ProductProcess? {
        val data = context.update(PRODUCT_PROCESS)
            .set(PRODUCT_PROCESS.PROCESS_CONVERT_CODE, request.processConvertCode)
            .set(PRODUCT_PROCESS.PROCESS_STATISTIC_CODE, request.processStatisticCode)
            .set(PRODUCT_PROCESS.PROCESS_INVENTORY_CODE, request.processInventoryCode)
            .set(PRODUCT_PROCESS.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
            .where(PRODUCT_PROCESS.ID.eq(request.id))
            .returningResult(PRODUCT_PROCESS)
            .fetchInto(ProductProcess::class.java).firstOrNull()
        return  data;
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "default" -> {
                PRODUCT_PROCESS.PROCESS_NAME
                PRODUCT_PROCESS.LAYER_CODE
            }
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

