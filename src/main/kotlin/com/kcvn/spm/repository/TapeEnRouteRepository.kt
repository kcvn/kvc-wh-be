package com.kcvn.spm.repository

import com.kcvn.spm.model.tables.pojos.TapeEnRoute
import com.kcvn.spm.model.tables.references.TAPE_EN_ROUTE
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class TapeEnRouteRepository(private val context: DSLContext)  {


    fun getTapeEnRouteForReport(productNameShortCut:  List<String>, startDate: OffsetDateTime?, endDate: OffsetDateTime?): List<TapeEnRoute>{


        var condition = DSL.noCondition().and(TAPE_EN_ROUTE.IS_DELETED.eq(false))

        if (endDate != null) {
            condition = condition.and(TAPE_EN_ROUTE.RESPONSE_DATE.le(endDate))
        }
        if (startDate != null) {
            condition = condition.and(TAPE_EN_ROUTE.RESPONSE_DATE.ge(startDate))
        }

        var specCondition = DSL.noCondition()
        for (shortcut in productNameShortCut) {
            specCondition = specCondition.or(TAPE_EN_ROUTE.SPEC.like("%$shortcut%"))
        }
        val query = context.selectFrom(TAPE_EN_ROUTE)
            .where(condition.and(specCondition))
        val data = query.fetchInto(TapeEnRoute::class.java)
        return data
    }

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
                    TAPE_EN_ROUTE.ORDER_PLACEMENT_MONTH,
                    TAPE_EN_ROUTE.EXPORT_TYPE,
                    TAPE_EN_ROUTE.SUPPLIER_CD,
                    TAPE_EN_ROUTE.PURCHASE_ORDER,
                    TAPE_EN_ROUTE.ITEM_CD,
                    TAPE_EN_ROUTE.DESCRIPTION,
                    TAPE_EN_ROUTE.SPEC,
                    TAPE_EN_ROUTE.ORDERED_QUANTITY,
                    TAPE_EN_ROUTE.QTY_UM,
                    TAPE_EN_ROUTE.OPU_P_FC,
                    TAPE_EN_ROUTE.OPU_P,
                    TAPE_EN_ROUTE.OP_DLV_DT,
                    TAPE_EN_ROUTE.TRANSMIT,
                    TAPE_EN_ROUTE.DELIVERED_QUANTITY,
                    TAPE_EN_ROUTE.RESPONSE_DATE,
                    TAPE_EN_ROUTE.ESTIMATED_DATE,
                    TAPE_EN_ROUTE.ESTIMATED_MONTH,
                    TAPE_EN_ROUTE.COUPON_CODE
                )
                .values(
                    tapeEnRoute.orderPlacementMonth,
                    tapeEnRoute.exportType,
                    tapeEnRoute.supplierCd,
                    tapeEnRoute.purchaseOrder,
                    tapeEnRoute.itemCd,
                    tapeEnRoute.description,
                    tapeEnRoute.spec,
                    tapeEnRoute.orderedQuantity,
                    tapeEnRoute.qtyUm,
                    tapeEnRoute.opuPFc,
                    tapeEnRoute.opuP,
                    tapeEnRoute.opDlvDt,
                    tapeEnRoute.transmit,
                    tapeEnRoute.deliveredQuantity,
                    tapeEnRoute.responseDate,
                    tapeEnRoute.estimatedDate,
                    tapeEnRoute.estimatedMonth,
                    tapeEnRoute.couponCode
                )
                .returningResult(TAPE_EN_ROUTE)
                .fetchOne()
                ?.into(tapeEnRoute::class.java)
        }
        return result
    }
}