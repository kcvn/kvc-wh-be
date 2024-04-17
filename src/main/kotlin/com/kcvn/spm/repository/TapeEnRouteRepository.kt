package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.TapeEnRoute
import com.kcvn.spm.model.tables.references.TAPE_EN_ROUTE
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class TapeEnRouteRepository(private val context: DSLContext)  {

    fun getTapeEnRouteList(couponCode:String): List<TapeEnRoute> {
        var condition: Condition = DSL.noCondition()
        if (couponCode.isNotEmpty()) {
            condition = condition.and(TAPE_EN_ROUTE.COUPON_CODE.eq(couponCode))
        }
        return context.selectFrom(TAPE_EN_ROUTE)
            .where(condition.and(TAPE_EN_ROUTE.IS_DELETED.eq(false)))
            .fetchInto(TapeEnRoute::class.java)
    }

    fun deleteTapeEnRouteList(tapeEnRoutes:List<TapeEnRoute>) {
        context.update(TAPE_EN_ROUTE)
            .set(TAPE_EN_ROUTE.IS_DELETED, true)
            .where(TAPE_EN_ROUTE.ID.`in`(tapeEnRoutes.map { it.id }))
            .execute()
    }



    fun addTapeEnRoute(tapeEnRoute: TapeEnRoute): TapeEnRoute? {
        var result: TapeEnRoute? = null
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            result = transactionalContext
                .insertInto(
                    TAPE_EN_ROUTE,
                    TAPE_EN_ROUTE.EXPORT_TYPE,
                    TAPE_EN_ROUTE.PURCHASE_ORDER,
                    TAPE_EN_ROUTE.ITEM_CODE,
                    TAPE_EN_ROUTE.UNIT,
                    TAPE_EN_ROUTE.DESCRIPTION,
                    TAPE_EN_ROUTE.SPEC,
                    TAPE_EN_ROUTE.TRANSMIT,
                    TAPE_EN_ROUTE.ORDERED_QUANTITY,
                    TAPE_EN_ROUTE.DELIVERED_QUANTITY,
                    TAPE_EN_ROUTE.RESPONSE_DATE,
                    TAPE_EN_ROUTE.TAPE_LOT,
                    TAPE_EN_ROUTE.QUANTITY,
                    TAPE_EN_ROUTE.NUMBER_OF_BOXES,
                    TAPE_EN_ROUTE.INTEGRATION_CONFIRMATION,
                    TAPE_EN_ROUTE.CONTACT_STATUS,
                    TAPE_EN_ROUTE.COUPON_CODE
                )
                .values(
                    tapeEnRoute.exportType,
                    tapeEnRoute.purchaseOrder,
                    tapeEnRoute.itemCode,
                    tapeEnRoute.unit,
                    tapeEnRoute.description,
                    tapeEnRoute.spec,
                    tapeEnRoute.transmit,
                    tapeEnRoute.orderedQuantity,
                    tapeEnRoute.deliveredQuantity,
                    tapeEnRoute.responseDate,
                    tapeEnRoute.tapeLot,
                    tapeEnRoute.quantity,
                    tapeEnRoute.numberOfBoxes,
                    tapeEnRoute.integrationConfirmation,
                    tapeEnRoute.contactStatus,
                    tapeEnRoute.couponCode
                )
                .returningResult(TAPE_EN_ROUTE)
                .fetchOne()
                ?.into(tapeEnRoute::class.java)
        }
        return result
    }
}