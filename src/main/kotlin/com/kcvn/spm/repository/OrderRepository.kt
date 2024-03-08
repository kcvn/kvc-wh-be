package com.kcvn.spm.repository

import com.kcvn.spm.app.order.payload.model.OrderDetailByDateModel
import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.report.quantityreport.payload.request.CalculateQuantityRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.OrderFilterType
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
            condition = condition.and(PRODUCT.NAME.contains(request?.productName))
        }
        if (!request?.frame_1.isNullOrBlank()) {
            condition = condition.and(PRODUCT.FRAME_1.contains(request?.frame_1))
        }
        if (!request?.srNosr.isNullOrBlank()) {
            condition = condition.and(PRODUCT.SR_NOSR.contains(request?.srNosr))
        }
        if (request?.filterType == OrderFilterType.DATE && request.startDate != null && request.endDate != null) {
            condition = condition.and(ORDER_DETAIL.ORDER_DATE.between(request.startDate, request.endDate))
        }
        if (request?.filterType == OrderFilterType.ORDER) {
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
            throw e
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

    fun getOrdersByCodeAndVersions(orderCode: String, versions: List<Int>?): List<Order> {
        val query = context.selectFrom(ORDER)
            .where(
                ORDER.ORDER_CODE.eq(orderCode)
                    .and(ORDER.IS_DELETED.eq(false))
            )
        if (versions != null) {
            if (versions.isNotEmpty()) {
                query.and(ORDER.VERSION.`in`(versions))
            }
        }
        return query.fetchInto(Order::class.java)
    }

    fun getOrderCodeByMonth(request: CalculateQuantityRequest): List<Order> {
        var condition = DSL.noCondition()
        if (request.endDate != null) {
            condition = condition.and(ORDER.END_DATE.le(request.endDate))
        }
        condition = condition.and(ORDER.START_DATE.ge(request.startDate)).and(ORDER.IS_DELETED.eq(false)).or(ORDER.START_DATE.le(request.endDate))
        return context.select(
            ORDER.ID,
            ORDER.ORDER_CODE,
            ORDER.VERSION,
            ORDER.START_DATE,
            ORDER.END_DATE
        ).from(ORDER)
            .where(condition)
            .orderBy(ORDER.START_DATE.sort(SortOrder.DESC))
            .fetchInto(Order::class.java)
    }

    fun getNameOrderByMonth(request: CalculateQuantityRequest) : List<String> {

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

    fun getOrderById(request: List<String>) : List<Order>{
        return context.selectFrom(ORDER)
            .where(ORDER.ID.`in`(request))
            .fetchInto(Order::class.java)
    }
}