package com.kcvn.spm.repository

import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.model.tables.pojos.OrderDetail
import com.kcvn.spm.model.tables.references.ORDER_DETAIL
import org.jooq.DSLContext
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository

@Repository
class OrderDetailRepository(private val context: DSLContext) {

    fun getIdProductOnOderDetailByOderVersionMax(request: List<String?>) : List<String?> {
        return context.select(
            ORDER_DETAIL.PRODUCT_ID
        )
            .from(ORDER_DETAIL)
            .where(ORDER_DETAIL.ORDER_ID.`in`(request))
            .fetchInto(String::class.java)
    }

    fun getOrderDetailByProductId (productIds: List<String?>, time: CalculateQuantityRequest) : List<OrderDetail> {
        val subQuery = DSL.select(
            ORDER_DETAIL.PRODUCT_ID,
            ORDER_DETAIL.ORDER_DATE,
            DSL.max(ORDER_DETAIL.CREATED_DATE)
        )
            .from(ORDER_DETAIL)
            .where(ORDER_DETAIL.PRODUCT_ID.`in`(productIds)
                .and(ORDER_DETAIL.ORDER_DATE.ge(time.startDate))
                .and(ORDER_DETAIL.ORDER_DATE.le(time.endDate)))
            .groupBy(ORDER_DETAIL.PRODUCT_ID, ORDER_DETAIL.ORDER_DATE)

        val condition = DSL.row(ORDER_DETAIL.PRODUCT_ID,
            ORDER_DETAIL.ORDER_DATE,
            ORDER_DETAIL.CREATED_DATE)
            .`in`(subQuery)
        return context.selectFrom(ORDER_DETAIL)
            .where(condition)
            .fetchInto(OrderDetail::class.java)
    }
}