package com.kcvn.spm.repository

import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.app.productprocess.payload.response.ImportProcessResponse
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.ProcessProcedureStructure
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.model.tables.references.PROCESS_PROCEDURE_STRUCTURE
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
class ProductProcessRepository(private val context: DSLContext) : SortingRepository()  {
    fun findByKeywordPaginated(keyword: String?,hasProcessConvertCode: Boolean, pageable: Pageable): Pair<List<ProductProcessResponse>, Int?>
    {
        var condition: Condition = DSL.noCondition()
        if(keyword != null){
            val lowerKeyword = DSL.lower(keyword);
            condition = condition.and(DSL.lower(PRODUCT.NAME).contains(lowerKeyword))
        }
        if(hasProcessConvertCode){
            condition = condition.and(PRODUCT_PROCESS.PROCESS_CONVERT_CODE.isNull
                .or(PRODUCT_PROCESS.PROCESS_STATISTIC_CODE.isNull))

        }
        val productProcessQuery = context
            .select(
                PRODUCT.NAME.`as`("productName"),
                PRODUCT_PROCESS.ID.`as`("processId"),
                PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE.`as`("layerCode"),
                PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.`as`("processCode"),
                PRODUCT_PROCESS.PROCESS_NAME_JP.`as`("processNameJp"),
                PRODUCT_PROCESS.PROCESS_NAME.`as`("processName"),
                PRODUCT_PROCESS.PROCESS_CONVERT_CODE.`as`("processConvertCode"),
                PRODUCT_PROCESS.PROCESS_STATISTIC_CODE.`as`("processStatisticCode"),
                PRODUCT_PROCESS.PROCESS_INVENTORY_CODE.`as`("processInventoryCode"),
                PRODUCT.ID.`as`("productId"),
            )
            .from(PRODUCT_PROCESS
                .join(PROCESS_PROCEDURE_STRUCTURE)
                    .on(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID
                    .eq(PROCESS_PROCEDURE_STRUCTURE.ID)))
                .join(PRODUCT)
                    .on(PRODUCT.NAME.eq(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE))
            .where(condition
                .and(PRODUCT_PROCESS.IS_DELETED.eq(false))
                .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, PRODUCT_PROCESS.CREATED_DATE))
            .limit(pageable.pageSize)
            .offset(pageable.offset)
            .fetchInto(ProductProcessResponse::class.java)
        val queryTotal =  context
        .selectCount()
        .from(PRODUCT_PROCESS)
        .join(PROCESS_PROCEDURE_STRUCTURE)
        .on(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID
            .eq(PROCESS_PROCEDURE_STRUCTURE.ID))
            .join(PRODUCT)
            .on(PRODUCT.NAME.eq(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE))
        .where(condition.and(PRODUCT_PROCESS.IS_DELETED.eq(false))
            .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false)))
        val totalCount = context.fetchOne(queryTotal)?.value1()
        return  Pair(productProcessQuery, totalCount);
    }

    fun getByProduct(productNames: List<String>) : List<ProductProcess> {
        return context.selectFrom(PRODUCT_PROCESS)
            //.where(PRODUCT_PROCESS.PRODUCT_NAME.`in`(productNames).and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
            .fetchInto(ProductProcess::class.java)
    }

    fun getByProductProcessDetail(productName: String?): List<ProductProcessResponse?>? {
        return context.select(
            PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.`as`("productName"),
            PRODUCT_PROCESS.ID.`as`("processId"),
            PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE.`as`("layerCode"),
            PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.`as`("processCode"),
            PRODUCT_PROCESS.PROCESS_NAME_JP.`as`("processNameJp"),
            PRODUCT_PROCESS.PROCESS_NAME.`as`("processName"),
            PRODUCT_PROCESS.PROCESS_CONVERT_CODE.`as`("processConvertCode"),
            PRODUCT_PROCESS.PROCESS_STATISTIC_CODE.`as`("processStatisticCode"),
            PRODUCT_PROCESS.PROCESS_INVENTORY_CODE.`as`("processInventoryCode"),
            PROCESS_PROCEDURE_STRUCTURE.PROCESS_SEQUENCE.`as`("processSequence"),
        )
            .from(PRODUCT_PROCESS
                .join(PROCESS_PROCEDURE_STRUCTURE)
                .on(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID
                    .eq(PROCESS_PROCEDURE_STRUCTURE.ID)))
            .where(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.eq(productName)
                .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false))
                .and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
            .orderBy(PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE, PROCESS_PROCEDURE_STRUCTURE.PROCESS_SEQUENCE)
            .fetchInto(ProductProcessResponse::class.java)
    }

    fun getByProductProcessDetailById(id: String?): ProductProcess? {
        val data = context.selectFrom(PRODUCT_PROCESS)
            .where((PRODUCT_PROCESS.ID.eq(id)).and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
            .fetchInto(ProductProcess::class.java).firstOrNull()
        return data
    }
    fun updateProductDetail(request: ProductProcess): ProductProcess? {
        return context.update(PRODUCT_PROCESS)
            .set(PRODUCT_PROCESS.PROCESS_CONVERT_CODE, request.processConvertCode)
            .set(PRODUCT_PROCESS.PROCESS_STATISTIC_CODE, request.processStatisticCode)
            .set(PRODUCT_PROCESS.PROCESS_INVENTORY_CODE, request.processInventoryCode)
            .set(PRODUCT_PROCESS.UPDATED_BY, CommonUtils.loggedInUser() ?: "SYSTEM")
            .where(PRODUCT_PROCESS.ID.eq(request.id).and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
            .returningResult(PRODUCT_PROCESS)
            .fetchAnyInto(ProductProcess::class.java);
    }

    fun getProductByFilter (request: ImportProcessRequest): ImportProcessResponse? {
            return  context.select(
                PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.`as`("productName"),
                PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.`as`("processCode"),
                PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE.`as`("layerCode"),
                PRODUCT_PROCESS.PROCESS_CONVERT_CODE.`as`("processConvertCode"),
                PRODUCT_PROCESS.PROCESS_STATISTIC_CODE.`as`("processStatisticCode"),
                PRODUCT_PROCESS.PROCESS_INVENTORY_CODE.`as`("processInventoryCode"),
                PRODUCT_PROCESS.ID.`as`("id"),
            )
                .from(PROCESS_PROCEDURE_STRUCTURE
                    .join(PRODUCT_PROCESS)
                    .on(PROCESS_PROCEDURE_STRUCTURE.ID.eq(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID)))
                .where(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.eq(request.productName)
                    .and(PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE.eq(request.layerCode))
                    .and(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.eq(request.processCode))
                    .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false))
                    .and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
                .fetchAnyInto(ImportProcessResponse::class.java)
    }


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "productName" -> {
                PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE
            }
            "layerCode" -> {
                PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE
            }
            "processConvertCode" -> {
                PRODUCT_PROCESS.PROCESS_CONVERT_CODE
            }
            "processCode" -> {
                PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE
            }
            "processName" -> {
                PRODUCT_PROCESS.PROCESS_NAME
            }
            "processNameJp" -> {
                PRODUCT_PROCESS.PROCESS_NAME_JP
            }
            "processStatisticCode" -> {
                PRODUCT_PROCESS.PROCESS_STATISTIC_CODE
            }
            "processInventoryCode" -> {
                PRODUCT_PROCESS.PROCESS_INVENTORY_CODE
            }
            "processSequence" -> {
                PROCESS_PROCEDURE_STRUCTURE.PROCESS_SEQUENCE
            }
            else -> {
                val errorMessage = java.lang.String.format("Could not find table field: $sortFieldName")
                throw InvalidDataAccessApiUsageException(errorMessage)
            }
        }
        return sortField
    }
}

