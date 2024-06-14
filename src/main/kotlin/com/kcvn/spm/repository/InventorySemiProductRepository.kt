package com.kcvn.spm.repository

import com.kcvn.spm.app.inventoryproduct.payload.request.InventorySemiProductSearchRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.InventorySemiProduct
import com.kcvn.spm.model.tables.references.INVENTORY_SEMI_PRODUCT
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class InventorySemiProductRepository(private val context: DSLContext) : SortingRepository() {

    fun getList(request: InventorySemiProductSearchRequest, pageable: Pageable, isExport: Boolean = false) : Pair<List<InventorySemiProduct>, Int> {
        var condition = INVENTORY_SEMI_PRODUCT.IS_DELETED.eq(false)
        if (!request.productName.isNullOrEmpty()) {
            condition = condition.and(INVENTORY_SEMI_PRODUCT.PRODUCT_NAME.containsIgnoreCase(request.productName!!.trim()))
        }
        if (request.startDate != null) {
            condition = condition.and(INVENTORY_SEMI_PRODUCT.INVENTORY_DATE.ge(request.startDate))
        }
        if (request.endDate != null) {
            condition = condition.and(INVENTORY_SEMI_PRODUCT.INVENTORY_DATE.le(request.endDate))
        }
        if (!request.tapeLotNo.isNullOrEmpty()) {
            condition = condition.and(INVENTORY_SEMI_PRODUCT.TAPE_LOT_NO.containsIgnoreCase(request.tapeLotNo!!.trim()))
        }

        val query = context.selectFrom(INVENTORY_SEMI_PRODUCT).where(condition)

        if (isExport) {
            val data = query
                .orderBy(getSortFields(pageable.sort, INVENTORY_SEMI_PRODUCT.INVENTORY_DATE))
                .fetchInto(InventorySemiProduct::class.java)

            return Pair(data, data.size)
        } else {
            val count = query.count()
            val data = query
                .orderBy(getSortFields(pageable.sort, INVENTORY_SEMI_PRODUCT.INVENTORY_DATE))
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(InventorySemiProduct::class.java)

            return Pair(data, count)
        }

    }

    fun isNoInventoryByDate(date: OffsetDateTime): Boolean {
        val data = context.selectFrom(INVENTORY_SEMI_PRODUCT)
            .where(INVENTORY_SEMI_PRODUCT.INVENTORY_DATE.eq(date)).and(INVENTORY_SEMI_PRODUCT.IS_DELETED.eq(false))
            .fetchAnyInto(InventorySemiProduct::class.java)

        return data == null
    }

    fun add(data: InventorySemiProduct) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.insertInto(
                INVENTORY_SEMI_PRODUCT,
                INVENTORY_SEMI_PRODUCT.INVENTORY_DATE,
                INVENTORY_SEMI_PRODUCT.PRODUCT_NAME,
                INVENTORY_SEMI_PRODUCT.TAPE_LOT_NO,
                INVENTORY_SEMI_PRODUCT.SET_QUANTITY,
                INVENTORY_SEMI_PRODUCT.BLOCK_QUANTITY,
                INVENTORY_SEMI_PRODUCT.SUM_BLOCK_QUANTITY,
                INVENTORY_SEMI_PRODUCT.NG_BLOCK_QUANTITY,
                INVENTORY_SEMI_PRODUCT.SUCCESS_BLOCK_QUANTITY,
                INVENTORY_SEMI_PRODUCT.CREATED_BY
            ).values(
                data.inventoryDate,
                data.productName,
                data.tapeLotNo,
                data.setQuantity,
                data.blockQuantity,
                data.sumBlockQuantity,
                data.ngBlockQuantity,
                data.successBlockQuantity,
                CommonUtils.loggedInUser() ?: Constants.SYSTEM
            ).execute()
        }
    }

    fun deleteByDate(date: OffsetDateTime) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(INVENTORY_SEMI_PRODUCT)
                .where(INVENTORY_SEMI_PRODUCT.INVENTORY_DATE.eq(date))
                .execute()
        }
    }


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        val sortField: TableField<*, *> = when (fieldName) {
            "inventorydate" -> INVENTORY_SEMI_PRODUCT.INVENTORY_DATE
            "productname" -> INVENTORY_SEMI_PRODUCT.PRODUCT_NAME
            "setquantity" -> INVENTORY_SEMI_PRODUCT.SET_QUANTITY
            "blockquantity" -> INVENTORY_SEMI_PRODUCT.BLOCK_QUANTITY
            "sumblockquantity" -> INVENTORY_SEMI_PRODUCT.SUM_BLOCK_QUANTITY
            "ngblockquantity" -> INVENTORY_SEMI_PRODUCT.NG_BLOCK_QUANTITY
            "successblockquantity" -> INVENTORY_SEMI_PRODUCT.SUCCESS_BLOCK_QUANTITY
            else -> INVENTORY_SEMI_PRODUCT.INVENTORY_DATE
        }
        return  sortField
    }
}