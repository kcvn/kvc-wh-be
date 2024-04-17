package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.TapeInventory
import com.kcvn.spm.model.tables.references.TAPE_INVENTORY
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository

class TapeInventoryRepository(private val context: DSLContext) {
    fun getTapeEnRouteListByDate(stocktakingDay:OffsetDateTime?): List<TapeInventory> {
        var condition: Condition = DSL.noCondition()
        if(stocktakingDay!=null){
            condition = condition.and(TAPE_INVENTORY.STOCKTAKING_DAY.eq(stocktakingDay))
        }
        return context.selectFrom(TAPE_INVENTORY)
            .where(condition.and(TAPE_INVENTORY.IS_DELETED.eq(false)))
            .fetchInto(TapeInventory::class.java)
    }
    fun deleteTapeInventoryList(tapeInventories:List<TapeInventory>) {
        context.update(TAPE_INVENTORY)
            .set(TAPE_INVENTORY.IS_DELETED, true)
            .where(TAPE_INVENTORY.ID.`in`(tapeInventories.map { it.id }))
            .execute()
    }



    fun add(tapeInventory: TapeInventory): TapeInventory? {
        var result: TapeInventory? = null
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            result = transactionalContext
                .insertInto(
                    TAPE_INVENTORY,
                    TAPE_INVENTORY.EXPORT_TYPE,
                    TAPE_INVENTORY.PRODUCT_NAME_SHORT_CUT,
                    TAPE_INVENTORY.PRODUCT_NAME,
                    TAPE_INVENTORY.TAPE_IN_WAREHOUSE,
                    TAPE_INVENTORY.TAPE_IN_DEPARTMENT,
                    TAPE_INVENTORY.TAPE_NG,
                    TAPE_INVENTORY.STOCKTAKING_DAY
                )
                .values(
                    tapeInventory.exportType,
                    tapeInventory.productNameShortCut,
                    tapeInventory.productName,
                    tapeInventory.tapeInWarehouse,
                    tapeInventory.tapeInDepartment,
                    tapeInventory.tapeNg,
                    tapeInventory.stocktakingDay
                )
                .returningResult(TAPE_INVENTORY)
                .fetchOne()
                ?.into(TapeInventory::class.java)
        }
        return result
    }
}