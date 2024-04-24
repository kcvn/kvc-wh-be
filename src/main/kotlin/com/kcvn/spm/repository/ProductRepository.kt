package com.kcvn.spm.repository

import com.kcvn.spm.app.product.payload.request.ProductSearchRequest
import com.kcvn.spm.app.product.payload.response.ProductDetailResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.model.tables.references.COMPLETION_RATE_PRODUCT
import com.kcvn.spm.model.tables.references.PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime
import java.time.ZoneOffset


@Repository
class ProductRepository(private val context: DSLContext) : SortingRepository() {

    fun getPagingList(request: ProductSearchRequest?, pageable: Pageable): Pair<List<Product>, Int> {
        var condition: Condition = DSL.noCondition()
        if (request != null) {
            if (!request.search.isNullOrEmpty()) condition =
                condition.and(PRODUCT.NAME.containsIgnoreCase(request.search?.lowercase()))

            if (!request.frame_1.isNullOrEmpty()) condition = condition.and(PRODUCT.FRAME_1.eq(request.frame_1))

            if (!request.frame_2.isNullOrEmpty()) condition = condition.and(PRODUCT.FRAME_2.eq(request.frame_2))

            if (!request.mold.isNullOrEmpty()) condition = condition.and(PRODUCT.MOLD.eq(request.mold))

            if (!request.exportType.isNullOrEmpty()) condition =
                condition.and(PRODUCT.EXPORT_TYPE.eq(request.exportType))

            if (!request.srNosr.isNullOrEmpty()) condition = condition.and(PRODUCT.SR_NOSR.eq(request.srNosr))

            if (!request.tapeType.isNullOrEmpty()) condition = condition.and(PRODUCT.TAPE_TYPE.eq(request.tapeType))
        }

        val data = context.selectFrom(PRODUCT)
            .where(condition.and(PRODUCT.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, PRODUCT.CREATED_DATE))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(Product::class.java)

        val total = context.fetchCount(PRODUCT, condition.and(PRODUCT.IS_DELETED.eq(false)))

        return Pair(data, total)
    }

    fun getList(request: ProductSearchRequest?, pageable: Pageable): List<Product> {
        var condition: Condition = DSL.noCondition()
        if (request != null) {
            if (!request.search.isNullOrEmpty()) condition = condition.and(PRODUCT.NAME.contains(request.search))

            if (!request.frame_1.isNullOrEmpty()) condition = condition.and(PRODUCT.FRAME_1.eq(request.frame_1))

            if (!request.frame_2.isNullOrEmpty()) condition = condition.and(PRODUCT.FRAME_1.eq(request.frame_2))

            if (!request.mold.isNullOrEmpty()) condition = condition.and(PRODUCT.MOLD.eq(request.mold))

            if (!request.exportType.isNullOrEmpty()) condition =
                condition.and(PRODUCT.EXPORT_TYPE.eq(request.exportType))

            if (!request.srNosr.isNullOrEmpty()) condition = condition.and(PRODUCT.SR_NOSR.eq(request.srNosr))

            if (!request.tapeType.isNullOrEmpty()) condition = condition.and(PRODUCT.TAPE_TYPE.eq(request.tapeType))
        }

        return context.selectFrom(PRODUCT)
            .where(condition.and(PRODUCT.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, PRODUCT.CREATED_DATE))
            .fetchInto(Product::class.java)

    }

    fun getProductDetail(request: String?): ProductDetailResponse? {
        val data = context.selectFrom(
            PRODUCT
                .leftJoin(COMPLETION_RATE_PRODUCT)
                .on(PRODUCT.NAME.eq(COMPLETION_RATE_PRODUCT.PRODUCT_NAME))
        )
            .where(
                PRODUCT.NAME.eq(request)
                    .and(PRODUCT.IS_DELETED.eq(false))
            )
            .orderBy(COMPLETION_RATE_PRODUCT.EXPIRATION_DATE.desc())
            .limit(1)
            .fetchAnyInto(ProductDetailResponse::class.java)
        return data
    }


    fun getByName(names: List<String>): List<Product> {
        return context.selectFrom(PRODUCT)
            .where(PRODUCT.NAME.`in`(names).and(PRODUCT.IS_DELETED.eq(false)))
            .fetchInto(Product::class.java)
    }


    fun getListNameProduct(): List<String> {
        return context.select(PRODUCT.NAME)
            .from(PRODUCT)
            .where(PRODUCT.IS_DELETED.eq(false))
            .fetchInto(String::class.java)
    }

    fun getProductList(): List<Product> {
        return context.select(PRODUCT)
            .from(PRODUCT)
            .where(PRODUCT.IS_DELETED.eq(false))
            .fetchInto(Product::class.java)
    }

    fun add(data: Product) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.insertInto(
                PRODUCT,
                PRODUCT.NAME,
                PRODUCT.EXPORT_TYPE,
                PRODUCT.SIZE,
                PRODUCT.FRAME_1,
                PRODUCT.FRAME_2,
                PRODUCT.MOLD,
                PRODUCT.PRODUCT_LINE,
                PRODUCT.SR_NOSR,
                PRODUCT.PCS_SH,
                PRODUCT.SH_BLOCK,
                PRODUCT.LAYER_COUNT,
                PRODUCT.RING_JIG,
                PRODUCT.PROCESS,
                PRODUCT.SNAP_MOLD,
                PRODUCT.TAPE_COMMON,
                PRODUCT.TAPE_TYPE,
                PRODUCT.PRODUCT_LAYER_DETAIL,
                PRODUCT.CREATED_BY
            ).values(
                data.name,
                data.exportType,
                data.size,
                data.frame_1,
                data.frame_2,
                data.mold,
                data.productLine,
                data.srNosr,
                data.pcsSh,
                data.shBlock,
                data.layerCount,
                data.ringJig,
                data.process,
                data.snapMold,
                data.tapeCommon,
                data.tapeType,
                data.productLayerDetail,
                CommonUtils.loggedInUser() ?: Constants.SYSTEM
            ).execute()
        }
    }

    fun update(data: Product) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.update(PRODUCT)
                .set(PRODUCT.NAME, data.name)
                .set(PRODUCT.EXPORT_TYPE, data.exportType)
                .set(PRODUCT.SIZE, data.size)
                .set(PRODUCT.FRAME_1, data.frame_1)
                .set(PRODUCT.FRAME_2, data.frame_2)
                .set(PRODUCT.MOLD, data.mold)
                .set(PRODUCT.PRODUCT_LINE, data.productLine)
                .set(PRODUCT.SR_NOSR, data.srNosr)
                .set(PRODUCT.PCS_SH, data.pcsSh)
                .set(PRODUCT.SH_BLOCK, data.shBlock)
                .set(PRODUCT.LAYER_COUNT, data.layerCount)
                .set(PRODUCT.RING_JIG, data.ringJig)
                .set(PRODUCT.PROCESS, data.process)
                .set(PRODUCT.SNAP_MOLD, data.snapMold)
                .set(PRODUCT.TAPE_COMMON, data.tapeCommon)
                .set(PRODUCT.TAPE_TYPE, data.tapeType)
                .set(PRODUCT.PRODUCT_LAYER_DETAIL, data.productLayerDetail)
                .set(PRODUCT.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(PRODUCT.UPDATED_DATE, OffsetDateTime.now(ZoneOffset.UTC))
                .where(PRODUCT.ID.eq(data.id))
                .execute()
        }
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "name" -> PRODUCT.NAME
            "exporttype" -> PRODUCT.EXPORT_TYPE
            "size" -> PRODUCT.SIZE
            "createdDate" -> PRODUCT.CREATED_DATE
            "frame_1" -> PRODUCT.FRAME_1
            "frame_2" -> PRODUCT.FRAME_2
            "mold" -> PRODUCT.MOLD
            "productline" -> PRODUCT.PRODUCT_LINE
            "srnosr" -> PRODUCT.SR_NOSR
            "pcssh" -> PRODUCT.PCS_SH
            "blocksh" -> PRODUCT.SH_BLOCK
            "layercount" -> PRODUCT.LAYER_COUNT
            else -> PRODUCT.CREATED_DATE
        }
        return sortField
    }

    fun getProductDetailWithCompletionRateByNames(productNames: List<String?>): List<ProductDetailResponse?> {
        val data = context.select(
            PRODUCT.ID,
            PRODUCT.NAME,
            PRODUCT.EXPORT_TYPE,
            PRODUCT.SIZE,
            PRODUCT.FRAME_1,
            PRODUCT.FRAME_2,
            PRODUCT.MOLD,
            PRODUCT.PRODUCT_LINE,
            PRODUCT.SR_NOSR,
            PRODUCT.PCS_SH,
            PRODUCT.SH_BLOCK,
            PRODUCT.LAYER_COUNT,
            PRODUCT.RING_JIG,
            PRODUCT.PROCESS,
            PRODUCT.SNAP_MOLD,
            PRODUCT.TAPE_COMMON,
            PRODUCT.TAPE_TYPE,
            PRODUCT.PRODUCT_LAYER_DETAIL,
            COMPLETION_RATE_PRODUCT.RATE,
            COMPLETION_RATE_PRODUCT.EFFECTIVE_DATE,
            COMPLETION_RATE_PRODUCT.EXPIRATION_DATE
        ).from(PRODUCT)
            .leftJoin(COMPLETION_RATE_PRODUCT)
            .on(PRODUCT.NAME.eq(COMPLETION_RATE_PRODUCT.PRODUCT_NAME))
            .where(PRODUCT.NAME.`in`(productNames).and(PRODUCT.IS_DELETED.eq(false)))
            .orderBy(COMPLETION_RATE_PRODUCT.EXPIRATION_DATE.desc())
            .fetchInto(ProductDetailResponse::class.java)
        return data
    }

    fun getByIds(productIDs: List<String?>): List<Product> {
        return context.selectFrom(PRODUCT)
            .where(PRODUCT.ID.`in`(productIDs).and(PRODUCT.IS_DELETED.eq(false)))
            .fetchInto(Product::class.java)
    }
}