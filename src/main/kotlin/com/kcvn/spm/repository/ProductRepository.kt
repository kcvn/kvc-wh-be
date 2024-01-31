package com.kcvn.spm.repository

import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.AuthUser
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.model.tables.references.AUTH_ROLE
import com.kcvn.spm.model.tables.references.AUTH_USER
import com.kcvn.spm.model.tables.references.PRODUCT
import com.kcvn.spm.model.tables.references.PRODUCT_PROCESS
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class ProductRepository (private val context: DSLContext) : SortingRepository(){

    fun getList(request: ProductSearchRequest?, pageable: Pageable) : Pair<List<Product>, Int> {
        var condition: Condition = DSL.noCondition()
        if (request != null) {
            if (!request.search.isNullOrEmpty()) condition = condition.and(PRODUCT.NAME.contains(request.search))

            if (!request.frame_1.isNullOrEmpty()) condition = condition.and(PRODUCT.FRAME_1.eq(request.frame_1))

            if (!request.frame_2.isNullOrEmpty()) condition = condition.and(PRODUCT.FRAME_1.eq(request.frame_2))

            if (!request.mold.isNullOrEmpty()) condition = condition.and(PRODUCT.MOLD.eq(request.mold))

            if (!request.exportType.isNullOrEmpty()) condition = condition.and(PRODUCT.EXPORT_TYPE.eq(request.exportType))

            if (!request.srNosr.isNullOrEmpty()) condition = condition.and(PRODUCT.SR_NOSR.eq(request.srNosr))

            if (!request.tapeType.isNullOrEmpty()) condition = condition.and(PRODUCT.TAPE_TYPE.eq(request.tapeType))
        }
        var data = context.selectFrom(PRODUCT)
            .where(condition.and(PRODUCT.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, PRODUCT.CREATED_DATE))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(Product::class.java)

        val total = context.fetchCount(PRODUCT, condition.and(PRODUCT.IS_DELETED.eq(false)))

        return Pair(data, total)
    }

    fun getProductDetail(request: String) : Product? {
        val data = context.selectFrom((PRODUCT))
            .where(PRODUCT.ID.eq(request))
            .orderBy(PRODUCT_PROCESS.LAYER_CODE)
            .fetchAnyInto(Product::class.java)
        return data;
    }

    fun getProductByListName(request: List<String?>) : List<Product>? {
        val data = context.selectFrom(PRODUCT)
            .where(PRODUCT.NAME.`in`(request))
            .fetchInto(Product::class.java)
        return data
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "name" -> { PRODUCT.NAME }
            "exporttype" -> { PRODUCT.EXPORT_TYPE }
            "size" -> { PRODUCT.SIZE }
            "createdDate" -> { PRODUCT.CREATED_DATE }
            "frame_1" -> { PRODUCT.FRAME_1 }
            "frame_2" -> { PRODUCT.FRAME_2 }
            "mold" -> { PRODUCT.MOLD }
            "productline" -> { PRODUCT.PRODUCT_LINE }
            "srnosr" -> { PRODUCT.SR_NOSR }
            "pcssh" -> { PRODUCT.PCS_SH }
            "blocksh" -> { PRODUCT.SH_BLOCK }
            "layercount" -> { PRODUCT.LAYER_COUNT }
            else -> {
                //val errorMessage = java.lang.String.format("Could not find table field: $sortFieldName")
                //throw InvalidDataAccessApiUsageException(errorMessage)
                PRODUCT.CREATED_DATE
            }
        }
        return sortField
    }
}