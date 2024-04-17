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
}