package com.kcvn.spm.repository

import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.model.tables.pojos.OrderDetail
import com.kcvn.spm.model.tables.references.ORDER
import com.kcvn.spm.model.tables.references.ORDER_DETAIL
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.format.DateTimeFormatter

@Repository
class OrderDetailRepository(private val context: DSLContext) {


    fun GetCalenderOrderDetail(orderId: String, productId: String): List<KeyValueResponse> {
        val result = context.select(
            ORDER_DETAIL.ORDER_DATE,
            ORDER_DETAIL.QUANTITY
        )
            .from(ORDER_DETAIL)
            .where(ORDER_DETAIL.ORDER_ID.eq(orderId).and(ORDER_DETAIL.PRODUCT_ID.eq(productId)))
            .fetch()

        return result.map { record ->
            KeyValueResponse(
                key = record[ORDER_DETAIL.ORDER_DATE]?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                value = record[ORDER_DETAIL.QUANTITY].toString()
            )
        }
    }

    fun getOrderDetailsByOrderIdsAndMonth(ids: List<String?>, request: CalculateQuantityRequest): List<OrderDetail> {
        var condition = DSL.noCondition()
        if (request.endDate != null) {
            condition = condition.and(ORDER_DETAIL.ORDER_DATE.le(request.endDate))
        }
        condition = condition.and(ORDER_DETAIL.ORDER_DATE.ge(request.startDate)).and(ORDER_DETAIL.IS_DELETED.eq(false))
        return context.select().from(ORDER_DETAIL)
            .where(ORDER_DETAIL.ORDER_ID.`in`(ids).and(condition).and(ORDER_DETAIL.IS_DELETED.eq(false)))
            .orderBy(ORDER_DETAIL.ORDER_DATE.sort(SortOrder.DESC))
            .fetchInto(OrderDetail::class.java)
    }
}