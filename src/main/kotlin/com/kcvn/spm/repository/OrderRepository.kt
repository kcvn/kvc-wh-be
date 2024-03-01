package com.kcvn.spm.repository

import com.kcvn.spm.app.order.payload.model.OrderDetailByDateModel
import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Order
import com.kcvn.spm.model.tables.pojos.OrderDetail
import com.kcvn.spm.model.tables.references.ORDER
import com.kcvn.spm.model.tables.references.ORDER_DETAIL
import com.kcvn.spm.model.tables.references.PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.SortOrder
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class OrderRepository(
    private val context: DSLContext,
) : SortingRepository() {

    fun getPagingListOrder(request: OrderSearchRequest?, pageable: Pageable?): Pair<List<OrderDetailModel>, Int> {
        var condition: Condition = DSL.noCondition()
        if (!request?.productName.isNullOrBlank()) {
            condition = condition.and(PRODUCT.NAME.containsIgnoreCase(request?.productName))
        }
        if (!request?.frame_1.isNullOrBlank()) {
            condition = condition.and(PRODUCT.FRAME_1.containsIgnoreCase(request?.frame_1))
        }
        if (!request?.srNosr.isNullOrBlank()) {
            condition = condition.and(PRODUCT.SR_NOSR.containsIgnoreCase(request?.srNosr))
        }
        if (request?.filterType == 0 && request.startDate != null && request.endDate != null) {
            condition = condition.and(ORDER_DETAIL.ORDER_DATE.between(request.startDate, request.endDate))
        }
        if (request?.filterType == 1) {
            if (!request.orderCode.isNullOrBlank()) {
                condition = condition.and(ORDER.ORDER_CODE.eq(request.orderCode))
            }
            if (!request.version.isNullOrEmpty()) {
                val versions = request.version?.split(",")?.map { x -> x.toInt() }
                if (!versions.isNullOrEmpty())
                    condition = condition.and(ORDER.VERSION.`in`(versions))
            }
        }

        condition = condition.and(ORDER_DETAIL.IS_DELETED.eq(false))
        val query = context.select(
            ORDER.ID.`as`("orderId"),
            PRODUCT.ID.`as`("productId"),
            PRODUCT.NAME.`as`("productName"),
            DSL.sum(ORDER_DETAIL.QUANTITY).`as`("quantity"),
            PRODUCT.FRAME_1.`as`("frame_1"),
            PRODUCT.LAYER_COUNT.`as`("layerCount"),
            PRODUCT.PCS_SH.`as`("pcsSh"),
            PRODUCT.SH_BLOCK.`as`("shBlock"),
            PRODUCT.SR_NOSR.`as`("srNosr"),
            ORDER.VERSION.`as`("version"),
        ).from(ORDER_DETAIL).join(ORDER).on(ORDER_DETAIL.ORDER_ID.eq(ORDER.ID).and(ORDER.IS_DELETED.eq(false)))
            .join(PRODUCT).on(ORDER_DETAIL.PRODUCT_ID.eq(PRODUCT.ID).and(PRODUCT.IS_DELETED.eq(false)))
            .where(condition)
            .groupBy(
                ORDER.ID,
                PRODUCT.ID,
                PRODUCT.FRAME_1,
                PRODUCT.LAYER_COUNT,
                PRODUCT.PCS_SH,
                PRODUCT.SH_BLOCK,
                PRODUCT.SR_NOSR,
                ORDER.VERSION
            )

        val count = query.count()
        val data = query
            .orderBy(getSortFields(pageable?.sort, ORDER.ORDER_CODE))
            .limit(pageable?.pageSize ?: 10)
            .offset(pageable?.offset ?: 0)
            .fetchInto(OrderDetailModel::class.java)

        return Pair(data, count)
    }

    fun getQuantityByCalendar(orderProductIds: List<Pair<String, String>>): List<OrderDetailByDateModel> {
        val orderIds = orderProductIds.map { x -> x.first }
        val productIds = orderProductIds.map { x -> x.second }
        val data = context.selectFrom(ORDER_DETAIL)
            .where(
                ORDER_DETAIL.ORDER_ID.`in`(orderIds).and(ORDER_DETAIL.PRODUCT_ID.`in`(productIds))
                    .and(ORDER_DETAIL.IS_DELETED.eq(false))
            )
            .fetchInto(OrderDetail::class.java)
            .map { x ->
                OrderDetailByDateModel(
                    x.orderId,
                    x.productId,
                    DateTimeHelper.toString(x.orderDate!!, DateTimeFormat.MM_dd_yyyy),
                    x.quantity
                )
            }
        return data
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        return when (sortFieldName) {
            "id" -> ORDER.ID
            "createdDate" -> ORDER.CREATED_DATE
            "version" -> ORDER.VERSION
            "productName" -> PRODUCT.NAME
            "frame1" -> PRODUCT.FRAME_1
            "layerCount" -> PRODUCT.LAYER_COUNT
            "pcsSh" -> PRODUCT.PCS_SH
            "shBlock" -> PRODUCT.SH_BLOCK
            "srNosR" -> PRODUCT.SR_NOSR
            else -> throw IllegalArgumentException("Could not find table field: $sortFieldName")
        }
    }


    fun getOrderCode(startDate: OffsetDateTime, endDate: OffsetDateTime?): List<Order> {
        var condition = DSL.noCondition()
        if (endDate != null) {
            condition = condition.and(ORDER.END_DATE.le(endDate))
        }
        condition = condition.and(ORDER.START_DATE.ge(startDate)).and(ORDER.IS_DELETED.eq(false))
        return context.select(
            ORDER.ORDER_CODE,
            ORDER.VERSION,
            ORDER.START_DATE,
            ORDER.END_DATE
        ).from(ORDER).where(condition).fetchInto(Order::class.java)
    }

    fun addOrder(order: Order, orderDetails: List<OrderDetail>) {
        DSL.startTransaction()
        try {
            val orderInsert = context.insertInto(
                ORDER,
                ORDER.ORDER_CODE,
                ORDER.START_DATE,
                ORDER.END_DATE,
                ORDER.VERSION,
                ORDER.CREATED_BY
            ).values(order.orderCode, order.startDate, order.endDate, order.version, CommonUtils.loggedInUser() ?: Constants.SYSTEM)
            .returningResult(ORDER).fetchInto(Order::class.java).firstOrNull()

            if (orderInsert != null) {
                for (orderDetail in orderDetails) {
                    context.insertInto(
                        ORDER_DETAIL,
                        ORDER_DETAIL.ORDER_ID,
                        ORDER_DETAIL.PRODUCT_ID,
                        ORDER_DETAIL.ORDER_DATE,
                        ORDER_DETAIL.QUANTITY,
                        ORDER_DETAIL.CREATED_BY
                    )
                        .values(
                            orderInsert.id,
                            orderDetail.productId,
                            orderDetail.orderDate,
                            orderDetail.quantity,
                            CommonUtils.loggedInUser() ?: Constants.SYSTEM
                        )
                        .returningResult(ORDER_DETAIL).fetchInto(OrderDetail::class.java).firstOrNull()
                }
            }
        } catch (e: Exception) {
            DSL.rollback()
        }
    }


    fun getOverlapOrderDate(startDate: OffsetDateTime, endDate: OffsetDateTime, orderCode: String): Order? {
        var condition = DSL.noCondition()
        condition = condition.and(ORDER.ORDER_CODE.notEqual(orderCode)).and(ORDER.IS_DELETED.eq(false))
            .and(
                (ORDER.END_DATE.ge(startDate).and(ORDER.END_DATE.lt(endDate)))
                    .or(ORDER.START_DATE.le(endDate).and(ORDER.END_DATE.gt(endDate)))
            )
        return context.selectFrom(ORDER).where(condition).fetchInto(Order::class.java).firstOrNull()
    }

    fun getByOrderCode(orderCode: String): Order? {
        return context.selectFrom(ORDER).where(ORDER.ORDER_CODE.eq(orderCode)).and(ORDER.IS_DELETED.eq(false))
            .orderBy(ORDER.VERSION.sort(SortOrder.DESC))
            .fetchInto(Order::class.java).firstOrNull()
    }

    fun getOrdersByCodeAndVersions(orderCode: String, versions: List<Int>): List<Order> {
        return context.selectFrom(ORDER)
            .where(
                ORDER.ORDER_CODE.eq(orderCode)
                    .and(ORDER.IS_DELETED.eq(false))
                    .and(ORDER.VERSION.`in`(versions))
            )
            .fetchInto(Order::class.java)
    }
}