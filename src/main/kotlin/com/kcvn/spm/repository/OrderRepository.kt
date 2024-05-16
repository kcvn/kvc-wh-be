package com.kcvn.spm.repository

import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Order
import com.kcvn.spm.model.tables.pojos.OrderDetail
import com.kcvn.spm.model.tables.references.ORDER
import com.kcvn.spm.model.tables.references.ORDER_DETAIL
import com.kcvn.spm.model.tables.references.PRODUCT
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime


@Repository
class OrderRepository(
    private val context: DSLContext
) : SortingRepository() {


    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        return when (fieldName) {
            "createddate" -> ORDER.CREATED_DATE
            "version" -> ORDER.VERSION
            "productname" -> PRODUCT.NAME
            "frame_1" -> PRODUCT.FRAME_1
            "layercount" -> PRODUCT.LAYER_COUNT
            else -> PRODUCT.NAME
        }
    }


    fun getOrderCode(startDate: OffsetDateTime?, endDate: OffsetDateTime?): List<Order> {
        var condition = DSL.noCondition()
        if (endDate != null) {
            condition = condition.and(ORDER.END_DATE.le(endDate))
        }
        condition = condition.and(ORDER.START_DATE.ge(startDate)).and(ORDER.IS_DELETED.eq(false))
        return context.select(
            ORDER.ID,
            ORDER.ORDER_CODE,
            ORDER.VERSION,
            ORDER.START_DATE,
            ORDER.END_DATE
        ).from(ORDER)
            .where(condition)
            .orderBy(ORDER.END_DATE.sort(SortOrder.DESC))
            .fetchInto(Order::class.java)
    }

    fun addOrder(order: Order, orderDetails: List<OrderDetail>) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val orderInsert = transactionalContext.insertInto(
                ORDER,
                ORDER.ORDER_CODE,
                ORDER.START_DATE,
                ORDER.END_DATE,
                ORDER.VERSION,
                ORDER.CREATED_BY
            ).values(order.orderCode, order.startDate, order.endDate, order.version, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
                .returningResult(ORDER).fetchInto(Order::class.java).firstOrNull()

            if (orderInsert != null) {
                val records = orderDetails.map { x ->
                    DSL.row(
                        orderInsert.id,
                        x.productId,
                        x.orderDate,
                        x.quantity,
                        CommonUtils.loggedInUser() ?: Constants.SYSTEM
                    )
                }
                transactionalContext.insertInto(
                    ORDER_DETAIL,
                    ORDER_DETAIL.ORDER_ID,
                    ORDER_DETAIL.PRODUCT_ID,
                    ORDER_DETAIL.ORDER_DATE,
                    ORDER_DETAIL.QUANTITY,
                    ORDER_DETAIL.CREATED_BY
                ).values(records).execute()
            }
        }
    }


    fun getOverlapOrderDate(startDate: OffsetDateTime, endDate: OffsetDateTime): Order? {
        var condition = DSL.noCondition()
        condition = condition.and(ORDER.IS_DELETED.eq(false))
            .and(
                (ORDER.END_DATE.ge(startDate).and(ORDER.END_DATE.le(endDate)))
                    .or(ORDER.START_DATE.le(endDate).and(ORDER.END_DATE.ge(endDate)))
            )
        return context.selectFrom(ORDER).where(condition).fetchInto(Order::class.java).firstOrNull()
    }

    fun getByOrderCode(orderCode: String): Order? {
        return context.selectFrom(ORDER).where(ORDER.ORDER_CODE.eq(orderCode)).and(ORDER.IS_DELETED.eq(false))
            .orderBy(ORDER.VERSION.sort(SortOrder.DESC))
            .fetchInto(Order::class.java).firstOrNull()
    }

    fun getNameOrderByMonth(request: CalculateQuantityRequest): List<String> {

        var condition = DSL.noCondition()
        condition = condition.or(ORDER.START_DATE.lt(request.startDate).and(ORDER.END_DATE.gt(request.endDate)))
            .or(ORDER.START_DATE.gt(request.startDate).and(ORDER.START_DATE.lt(request.endDate)))
            .or(ORDER.START_DATE.eq(request.startDate))
            .or(ORDER.START_DATE.eq(request.endDate))
            .or(ORDER.END_DATE.gt(request.startDate).and(ORDER.START_DATE.lt(request.endDate)))
            .or(ORDER.END_DATE.eq(request.startDate))
            .or(ORDER.END_DATE.eq(request.endDate))
        return context.select(
            ORDER.ORDER_CODE,
        )
            .from(ORDER)
            .where(condition)
            .fetchInto(String::class.java)
    }

    fun getIdOderVersionMax(request: List<String>): List<String> {
        val subQuery = DSL.select(
            ORDER.ORDER_CODE,
            DSL.max(ORDER.VERSION)
        )
            .from(ORDER)
            .where(ORDER.ORDER_CODE.`in`(request))
            .groupBy(ORDER.ORDER_CODE)

        val condition = DSL.row(ORDER.ORDER_CODE, ORDER.VERSION)
            .`in`(subQuery)

        return context.select(ORDER.ID)
            .from(ORDER)
            .where(condition)
            .fetchInto(String::class.java)
    }

    fun getOrderById(request: List<String>): List<Order> {
        return context.selectFrom(ORDER)
            .where(ORDER.ID.`in`(request))
            .fetchInto(Order::class.java)
    }
}