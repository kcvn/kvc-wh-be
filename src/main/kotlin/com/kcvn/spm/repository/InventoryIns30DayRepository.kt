package com.kcvn.spm.repository

import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.InventoryIns_30day
import com.kcvn.spm.model.tables.references.INVENTORY_INS_30DAY
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class InventoryIns30DayRepository(private val context: DSLContext) {

    fun add(data: InventoryIns_30day) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.insertInto(
                INVENTORY_INS_30DAY,
                INVENTORY_INS_30DAY.INVENTORY_DATE,
                INVENTORY_INS_30DAY.PRODUCT_NAME,
                INVENTORY_INS_30DAY.PROCESS_CODE,
                INVENTORY_INS_30DAY.LAYER_CODE,
                INVENTORY_INS_30DAY.CODE,
                INVENTORY_INS_30DAY.PRODUCT_QUANTITY,
                INVENTORY_INS_30DAY.CREATED_BY
            ).values(
                data.inventoryDate,
                data.productName,
                data.processCode,
                data.layerCode,
                data.code,
                data.productQuantity,
                CommonUtils.loggedInUser() ?: Constants.SYSTEM
            ).execute()
        }
    }

    fun deleteByDate(date: OffsetDateTime) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            transactionalContext.deleteFrom(INVENTORY_INS_30DAY)
                .where(INVENTORY_INS_30DAY.INVENTORY_DATE.eq(date))
                .execute()
        }
    }

    fun isNoInventoryByDate(date: OffsetDateTime): Boolean {
        val data = context.selectFrom(INVENTORY_INS_30DAY)
            .where(INVENTORY_INS_30DAY.INVENTORY_DATE.eq(date)).and(INVENTORY_INS_30DAY.IS_DELETED.eq(false))
            .fetchAnyInto(InventoryIns_30day::class.java)

        return data == null
    }

    fun getBy(
        productNames: List<String>,
        processCodes: List<String>,
        layerCodes: List<String>,
        codes: List<String>,
        invDates: List<OffsetDateTime>
    ): List<InventoryIns_30day> {
        val data = context.selectFrom(INVENTORY_INS_30DAY)
            .where(INVENTORY_INS_30DAY.PRODUCT_NAME.`in`(productNames))
            .and(INVENTORY_INS_30DAY.PROCESS_CODE.`in`(processCodes))
            .and(INVENTORY_INS_30DAY.LAYER_CODE.`in`(layerCodes))
            .and(INVENTORY_INS_30DAY.CODE.`in`(codes))
            .and(INVENTORY_INS_30DAY.INVENTORY_DATE.`in`(invDates))
            .and(INVENTORY_INS_30DAY.IS_DELETED.eq(false))
            .fetchInto(InventoryIns_30day::class.java)

        return data
    }
}