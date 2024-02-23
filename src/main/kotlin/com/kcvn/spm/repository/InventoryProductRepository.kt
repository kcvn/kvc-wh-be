package com.kcvn.spm.repository

import com.kcvn.spm.app.inventoryproduct.payload.request.InventoryProductRequest
import com.kcvn.spm.app.inventoryproduct.payload.response.InventoryProductResponse
import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.InventoryProduct
import com.kcvn.spm.model.tables.pojos.ProductProcess
import com.kcvn.spm.model.tables.pojos.WorkResult
import com.kcvn.spm.model.tables.references.*
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.dao.InvalidDataAccessApiUsageException
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class InventoryProductRepository(private val context: DSLContext) : SortingRepository()
{
    fun findDateInventoryProduct (date: OffsetDateTime) : InventoryProduct?{
        return context.selectFrom(INVENTORY_PRODUCT)
            .where(INVENTORY_PRODUCT.INVENTORY_DATE.eq(date))
            .fetchAnyInto(InventoryProduct::class.java)
    }

    fun findInventoryProduct(id: String?) : InventoryProduct? {
        return  context.selectFrom(INVENTORY_PRODUCT)
            .where(INVENTORY_PRODUCT.PROCESS_PROCEDURE_STRUCTURE_ID.eq(id))
            .fetchAnyInto(InventoryProduct::class.java)
    }

    fun insertInventoryProduct(request: InventoryProduct)  {
        val record = context.newRecord(INVENTORY_PRODUCT, request)
        context.insertInto(INVENTORY_PRODUCT).set(record).execute()
    }

    fun updateInventoryProduct(request: InventoryProduct)  {
        val record = context.newRecord(INVENTORY_PRODUCT, request)
        context.update(INVENTORY_PRODUCT).set(record)
            .where(INVENTORY_PRODUCT.PROCESS_PROCEDURE_STRUCTURE_ID
                .eq(record.processProcedureStructureId)).execute()
    }

    fun finByKeywordPaginated(request: InventoryProductRequest?, pageable: Pageable): Pair<List<InventoryProductResponse?>, Int?>{
        var condition: Condition = DSL.noCondition()

        if(request != null){
            if(!request.orderCode.isNullOrEmpty()){
                condition = condition.and(INVENTORY_PRODUCT.ORDER_CODE.contains(request.orderCode))
            }
            if(!request.productName.isNullOrEmpty()){
                condition = condition.and(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.contains(request.productName))
            }
            if(!request.listProcessGroup.isNullOrEmpty()){
                val processGroupCodes = request.listProcessGroup!!.split(",")
                var condition1 : Condition = DSL.noCondition()
                processGroupCodes.forEach { processGroup ->
                    condition1 = condition1.or(PROCESS_MASTER.GRP_PROCESS.eq(processGroup))
                }
                condition = condition.and(condition1)
            }
            if(!request.listProcessCode.isNullOrEmpty()){
                val processCodes = request.listProcessCode!!.split(",")
                var condition2 : Condition = DSL.noCondition()
                processCodes.forEach { processCode ->
                    condition2 = condition2.or(PROCESS_MASTER.PROCESS_CODE.eq(processCode))
                }
                condition = condition.and(condition2)
            }
            if(!request.tapeLot.isNullOrEmpty()){
                condition = condition.and(INVENTORY_PRODUCT.TAPE_LOT_NO.contains(request.tapeLot))
            }
            if(!request.code.isNullOrEmpty()){
                condition = condition.and((INVENTORY_PRODUCT.CODE.contains(request.code)))
            }
            if(request.fromDate != null && request.toDate != null){
                condition = condition.and(INVENTORY_PRODUCT.INVENTORY_DATE.between(request.fromDate, request.toDate))
            }
        }

        val data = context.select(
            INVENTORY_PRODUCT.INVENTORY_DATE.`as`("inventoryDate"),
            PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE.`as`("productName"),
            PROCESS_MASTER.PROCESS_NAME.`as`("processName"),
            PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.`as`("processCode"),
            PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE.`as`("layerCode"),
            PRODUCT.PCS_SH.`as`("pcsSh"),
            INVENTORY_PRODUCT.PRODUCT_QUANTITY.`as`("productQuantity"),
            INVENTORY_PRODUCT.SHEET_QUANTITY.`as`("sheetQuantity"),
            INVENTORY_PRODUCT.ORDER_CODE.`as`("orderCode"),
            INVENTORY_PRODUCT.TAPE_LOT_NO.`as`("tapeLotNo"),
            INVENTORY_PRODUCT.CODE.`as`("code"),
        )
            .from(INVENTORY_PRODUCT
            .join(PROCESS_PROCEDURE_STRUCTURE)
            .on(INVENTORY_PRODUCT.PROCESS_PROCEDURE_STRUCTURE_ID.eq(PROCESS_PROCEDURE_STRUCTURE.ID))
            .join(PROCESS_MASTER)
            .on(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.eq(PROCESS_MASTER.PROCESS_CODE))
            .join(PRODUCT)
            .on(PRODUCT.NAME.eq(PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE)))
            .where(condition.and(INVENTORY_PRODUCT.IS_DELETED.eq(false)))
            .orderBy(getSortFields(pageable.sort, INVENTORY_PRODUCT.CREATED_DATE))
            .limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(InventoryProductResponse::class.java)

        val totalData =  context
            .selectCount()
            .from(INVENTORY_PRODUCT
            .join(PROCESS_PROCEDURE_STRUCTURE)
            .on(INVENTORY_PRODUCT.PROCESS_PROCEDURE_STRUCTURE_ID.eq(PROCESS_PROCEDURE_STRUCTURE.ID))
            .join(PROCESS_MASTER)
            .on(PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE.eq(PROCESS_MASTER.PROCESS_CODE)))
            .where(condition.and(INVENTORY_PRODUCT.IS_DELETED.eq(false)))
        val total = context.fetchOne(totalData)?.value1()
        return  Pair(data, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val sortField: TableField<*, *> = when (sortFieldName) {
            "inventoryDate" -> {
                INVENTORY_PRODUCT.INVENTORY_DATE
            }
            "productName" -> {
                PROCESS_PROCEDURE_STRUCTURE.PRODUCT_CODE
            }
            "processName" -> {
                PROCESS_MASTER.PROCESS_NAME
            }
            "processCode" -> {
                PROCESS_PROCEDURE_STRUCTURE.PROCESS_CODE
            }
            "layerCode" -> {
                PROCESS_PROCEDURE_STRUCTURE.LAYER_CODE
            }
            "productQuantity" -> {
                INVENTORY_PRODUCT.PRODUCT_QUANTITY
            }
            "sheetQuantity" -> {
                INVENTORY_PRODUCT.SHEET_QUANTITY
            }
            "pcsSh" -> {
                PRODUCT.PCS_SH
            }
            "oderCode" -> {
                INVENTORY_PRODUCT.ORDER_CODE
            }
            "tapeLotno" -> {
                INVENTORY_PRODUCT.TAPE_LOT_NO
            }
            "code" -> {
                INVENTORY_PRODUCT.CODE
            }
            else -> {
                val errorMessage = java.lang.String.format("Could not find table field: $sortFieldName")
                throw InvalidDataAccessApiUsageException(errorMessage)
            }
        }
        return  sortField
    }
}