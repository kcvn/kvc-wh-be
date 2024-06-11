package com.kcvn.spm.repository

import com.kcvn.spm.app.productprocess.payload.model.ProductProcessModel
import com.kcvn.spm.app.productprocess.payload.request.ImportProcessRequest
import com.kcvn.spm.app.productprocess.payload.response.ImportProcessResponse
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Order
import com.kcvn.spm.model.tables.pojos.ProcessProcedureStructure
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.model.tables.references.*
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.Field
import org.jooq.SortField
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.stereotype.Repository
import java.time.Instant
import java.time.ZoneOffset

@Repository
class ProductProcessRepository(private val context: DSLContext) : SortingRepository() {
    fun findByKeywordPaginated(keyword: String?, hasProcessConvertCode: Boolean, pageable: Pageable): Pair<List<ProductProcessResponse?>, Int?> {
        var condition: Condition = DSL.noCondition()
        if (keyword?.trim() != null) {
            condition = condition.and(DSL.lower(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE).contains(keyword.trim().lowercase()))
        }
        if (hasProcessConvertCode) {
            condition = condition.and(PRODUCT_PROCESS.PROCESS_CONVERT_CODE.isNull
                .or(PRODUCT_PROCESS.PROCESS_STATISTIC_CODE.isNull))
        }

        var sortFields = getSortFields(pageable.sort, PRODUCT_PROCESS.CREATED_DATE).distinct().toMutableList()
        val sortLayerCode = pageable.sort.find { x -> x.property == "layerCode" }
        if (sortLayerCode != null) {
            if (sortLayerCode.direction == Sort.Direction.ASC)
                sortFields.add(1, DSL.cast(PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE, java.math.BigDecimal::class.java).asc())
            else
                sortFields.add(1, DSL.cast(PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE, java.math.BigDecimal::class.java).desc())
        }

        val productProcessQuery = context
            .select(
                PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.`as`("productName"),
                PRODUCT_PROCESS.ID.`as`("processId"),
                PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE.`as`("layerCode"),
                PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.`as`("processCode"),
                PRODUCT_PROCESS.PROCESS_CONVERT_CODE.`as`("processConvertCode"),
                PRODUCT_PROCESS.PROCESS_STATISTIC_CODE.`as`("processStatisticCode"),
                PRODUCT_PROCESS.PROCESS_INVENTORY_CODE.`as`("processInventoryCode"),
                PRODUCT.ID.`as`("productId"),
                PROCESS_PROCEDURE_STRUCTURE.ID.`as`("processProcedureStructureId"),
                PROCESS_PROCEDURE_STRUCTURE.PROCESS_SEQUENCE.`as`("processSequence"),
                PRODUCT_PROCESS.DAY_OF_IMPLEMENTATION,
                PRODUCT_PROCESS.INVENTORY_LAYER_GROUP
            )
            .from(PROCESS_PROCEDURE_STRUCTURE)
            .leftJoin(PRODUCT_PROCESS)
            .on(PROCESS_PROCEDURE_STRUCTURE.ID.eq(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID)
                .and(PRODUCT_PROCESS.IS_DELETED.eq(false))) // Điều kiện kết nối bảng PRODUCT_PROCESS
            .leftJoin(PRODUCT)
            .on(PRODUCT.NAME.eq(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE).and(PRODUCT.IS_DELETED.eq(false)))
            .where(condition.and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false))) // Điều kiện cho bảng PROCESS_PROCEDURE_STRUCTURE
            .orderBy(sortFields)
            .limit(pageable.pageSize)
            .offset(pageable.offset)
            .fetchInto(ProductProcessResponse::class.java)


        val queryTotal = context
            .selectCount()
            .from(PROCESS_PROCEDURE_STRUCTURE)
            .leftJoin(PRODUCT_PROCESS)
            .on(PROCESS_PROCEDURE_STRUCTURE.ID.eq(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID).and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
            .leftJoin(PRODUCT)
            .on(PRODUCT.NAME.eq(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE).and(PRODUCT.IS_DELETED.eq(false)))
            .where(condition.and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false)))

        val totalCount = context.fetchOne(queryTotal)?.value1()

        return Pair(productProcessQuery, totalCount);
    }

    fun getByProcessProcedureStructure(procedureStructureIds: List<String>): List<ProductProcess> {
        return context.selectFrom(PRODUCT_PROCESS)
            .where(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID.`in`(procedureStructureIds).and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
            .fetchInto(ProductProcess::class.java)
    }

    fun getByProductProcessDetail(productName: String?): List<ProductProcessResponse?>? {
        return context.select(
            PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.`as`("productName"),
            PRODUCT_PROCESS.ID.`as`("processId"),
            PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE.`as`("layerCode"),
            PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.`as`("processCode"),
            PROCESS_MASTER.PROCESS_NAME_JP.`as`("processNameJp"),
            PROCESS_MASTER.PROCESS_NAME.`as`("processName"),
            PRODUCT_PROCESS.PROCESS_CONVERT_CODE.`as`("processConvertCode"),
            PRODUCT_PROCESS.PROCESS_STATISTIC_CODE.`as`("processStatisticCode"),
            PRODUCT_PROCESS.PROCESS_INVENTORY_CODE.`as`("processInventoryCode"),
            PROCESS_PROCEDURE_STRUCTURE.PROCESS_SEQUENCE.`as`("processSequence"),
            PRODUCT_PROCESS.DAY_OF_IMPLEMENTATION,
            PRODUCT_PROCESS.INVENTORY_LAYER_GROUP
        )
            .from(PROCESS_PROCEDURE_STRUCTURE
                .leftJoin(PRODUCT_PROCESS)
                .on(PROCESS_PROCEDURE_STRUCTURE.ID
                    .eq(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID)
                    .and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
                .leftJoin(PROCESS_MASTER)
                .on(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE
                    .eq(PROCESS_MASTER.PROCESS_CODE)
                    .and(PROCESS_MASTER.IS_DELETED.eq(false)))
            )
            .where(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.eq(productName)
                .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false))
                .and(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.notEqual("0"))
                .and(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.notLike("0%"))
                //.and(PRODUCT_PROCESS.IS_DELETED.eq(false))
            )
            .orderBy(PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE, PROCESS_PROCEDURE_STRUCTURE.PROCESS_SEQUENCE)
            .fetchInto(ProductProcessResponse::class.java)
    }

    fun getByProductProcessDetailById(id: String?): ProductProcess? {
        val data = context.selectFrom(PRODUCT_PROCESS)
            .where((PRODUCT_PROCESS.ID.eq(id)).and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
            .fetchInto(ProductProcess::class.java).firstOrNull()
        return data
    }

    fun updateProcessDetail(request: ProductProcess): ProductProcess? {
        var result: ProductProcess? = null
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            result = transactionalContext.update(PRODUCT_PROCESS)
                .set(PRODUCT_PROCESS.PROCESS_CONVERT_CODE, request.processConvertCode)
                .set(PRODUCT_PROCESS.PROCESS_STATISTIC_CODE, request.processStatisticCode)
                .set(PRODUCT_PROCESS.PROCESS_INVENTORY_CODE, request.processInventoryCode)
                .set(PRODUCT_PROCESS.UPDATED_BY, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .set(PRODUCT_PROCESS.INVENTORY_LAYER_GROUP, request.inventoryLayerGroup)
                .set(PRODUCT_PROCESS.DAY_OF_IMPLEMENTATION, request.dayOfImplementation)
                .set(PRODUCT_PROCESS.UPDATED_DATE, Instant.now().atOffset(ZoneOffset.UTC))
                .where(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID.eq(request.processProcedureStructureId).and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
                .returningResult(PRODUCT_PROCESS)
                .fetchAnyInto(ProductProcess::class.java)
        }
        return result
    }

    fun insertProductProcess(request: ProductProcess) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val record = transactionalContext.newRecord(PRODUCT_PROCESS, request)
            transactionalContext.insertInto(PRODUCT_PROCESS).set(record).execute()
        }
    }

    fun addProductProcess(request: ProductProcess?): ProductProcess? {
        var result: ProductProcess? = null
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            result = transactionalContext.insertInto(PRODUCT_PROCESS,
                PRODUCT_PROCESS.PROCESS_CONVERT_CODE,
                PRODUCT_PROCESS.PROCESS_INVENTORY_CODE,
                PRODUCT_PROCESS.PROCESS_STATISTIC_CODE,
                PRODUCT_PROCESS.CREATED_BY,
                PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID,
                PRODUCT_PROCESS.INVENTORY_LAYER_GROUP,
                PRODUCT_PROCESS.DAY_OF_IMPLEMENTATION)
                .values(request?.processConvertCode,
                    request?.processInventoryCode,
                    request?.processStatisticCode,
                    CommonUtils.loggedInUser() ?: Constants.SYSTEM,
                    request?.processProcedureStructureId,
                    request?.inventoryLayerGroup,
                    request?.dayOfImplementation)
                .returningResult(PRODUCT_PROCESS).fetchInto(ProductProcess::class.java).firstOrNull()
        }
        return result
    }

    fun getProcessByFilter(request: ImportProcessRequest): ImportProcessResponse? {
        return context.select(
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

    fun findByIdProductProcedureStructure(id: String?): ProductProcess? {
        return context.selectFrom(PRODUCT_PROCESS)
            .where(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID.eq(id)
                .and(PRODUCT_PROCESS.IS_DELETED.eq(false)))
            .fetchAnyInto(ProductProcess::class.java)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "productName" -> PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE
            "processConvertCode" -> PRODUCT_PROCESS.PROCESS_CONVERT_CODE
            "processCode" -> PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE
            "processName" -> PRODUCT_PROCESS.PROCESS_NAME
            "processNameJp" -> PRODUCT_PROCESS.PROCESS_NAME_JP
            "processStatisticCode" -> PRODUCT_PROCESS.PROCESS_STATISTIC_CODE
            "processInventoryCode" -> PRODUCT_PROCESS.PROCESS_INVENTORY_CODE
            "processSequence" -> PROCESS_PROCEDURE_STRUCTURE.PROCESS_SEQUENCE
            else -> PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE
        }

        return sortField
    }

    fun add(productProcess: ProductProcess) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val record = transactionalContext.newRecord(PRODUCT_PROCESS, productProcess)
            transactionalContext.insertInto(PRODUCT_PROCESS).set(record).execute()
        }
    }

    fun getAll(): List<ProductProcess> {
        return context.selectFrom(PRODUCT_PROCESS)
            .where(PRODUCT_PROCESS.IS_DELETED.eq(false))
            .fetchInto(ProductProcess::class.java)
    }

    fun bulkInsertData(request: List<ProductProcess?>) {
        val dataChunks = request.chunked(100)
        for (chunkItem in dataChunks) {
            context.transaction { configuration ->
                val transactionalContext = DSL.using(configuration)
                val query = chunkItem.mapNotNull {
                    transactionalContext.insertInto(
                        PRODUCT_PROCESS,
                        PRODUCT_PROCESS.PROCESS_CONVERT_CODE,
                        PRODUCT_PROCESS.PROCESS_STATISTIC_CODE,
                        PRODUCT_PROCESS.PROCESS_INVENTORY_CODE,
                        PRODUCT_PROCESS.CREATED_DATE,
                        PRODUCT_PROCESS.CREATED_BY,
                        PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID,
                        PRODUCT_PROCESS.INVENTORY_LAYER_GROUP,
                        PRODUCT_PROCESS.DAY_OF_IMPLEMENTATION
                    ).values(
                        it?.processConvertCode,
                        it?.processStatisticCode,
                        it?.processInventoryCode,
                        it?.createdDate,
                        it?.createdBy,
                        it?.processProcedureStructureId,
                        it?.inventoryLayerGroup,
                        it?.dayOfImplementation
                    )
                }
                transactionalContext.batch(query).execute()
            }
        }
    }

    fun bulkUpdateData(request: List<ProductProcess>) {
        val dataChunks = request.chunked(100)
        for (chunkItem in dataChunks) {
            context.transaction { configuration ->
                val transactionalContext = DSL.using(configuration)

                val query = request.map { x ->
                    transactionalContext.update(PRODUCT_PROCESS)
                        .set(PRODUCT_PROCESS.PROCESS_NAME, x.processName)
                        .set(PRODUCT_PROCESS.PROCESS_NAME_JP, x.processNameJp)
                        .set(PRODUCT_PROCESS.PROCESS_CONVERT_CODE, x.processConvertCode)
                        .set(PRODUCT_PROCESS.PROCESS_STATISTIC_CODE, x.processStatisticCode)
                        .set(PRODUCT_PROCESS.PROCESS_INVENTORY_CODE, x.processInventoryCode)
                        .set(PRODUCT_PROCESS.UPDATED_DATE, Instant.now().atOffset(ZoneOffset.UTC))
                        .set(PRODUCT_PROCESS.UPDATED_BY, x.updatedBy)
                        .set(PRODUCT_PROCESS.INVENTORY_LAYER_GROUP, x.inventoryLayerGroup)
                        .set(PRODUCT_PROCESS.DAY_OF_IMPLEMENTATION, x.dayOfImplementation)
                        .where(PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID.eq(x.processProcedureStructureId))
                }
                transactionalContext.batch(query).execute()
            }
        }

    }

    fun getByProductNameForPlan(productNames: List<String>): List<ProductProcessModel> {
        var query = context.select(
            PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.`as`("productName"),
            PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE,
            PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE,
            PRODUCT_PROCESS.PROCESS_CONVERT_CODE,
            PRODUCT_PROCESS.PROCESS_STATISTIC_CODE,
            PRODUCT_PROCESS.PROCESS_INVENTORY_CODE,
            PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID,
            PROCESS_PROCEDURE_STRUCTURE.PROCESS_SEQUENCE,
            PRODUCT_PROCESS.INVENTORY_LAYER_GROUP,
            PRODUCT_PROCESS.DAY_OF_IMPLEMENTATION
        ).from(PRODUCT_PROCESS).join(PROCESS_PROCEDURE_STRUCTURE).on(
            PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID.eq(PROCESS_PROCEDURE_STRUCTURE.ID)
                .and(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.`in`(productNames))
                .and(PROCESS_PROCEDURE_STRUCTURE.IS_DELETED.eq(false))
        ).where(PRODUCT_PROCESS.IS_DELETED.eq(false))
            .groupBy(
                PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE,
                PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE,
                PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE,
                PRODUCT_PROCESS.PROCESS_CONVERT_CODE,
                PRODUCT_PROCESS.PROCESS_STATISTIC_CODE,
                PRODUCT_PROCESS.PROCESS_INVENTORY_CODE,
                PRODUCT_PROCESS.PROCESS_PROCEDURE_STRUCTURE_ID,
                PROCESS_PROCEDURE_STRUCTURE.PROCESS_SEQUENCE,
                PRODUCT_PROCESS.INVENTORY_LAYER_GROUP,
                PRODUCT_PROCESS.DAY_OF_IMPLEMENTATION,
            )

        val data = query.fetchInto(ProductProcessModel::class.java)

        return data.filter { it.processCode?.toIntOrNull() != 0 }
    }
}

