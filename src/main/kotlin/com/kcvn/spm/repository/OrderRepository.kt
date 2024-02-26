package com.kcvn.spm.repository

import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.order.payload.response.OrderCodeResponse
import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.Order
import com.kcvn.spm.model.tables.pojos.OrderDetail
import com.kcvn.spm.model.tables.references.ORDER
import com.kcvn.spm.model.tables.references.ORDER_DETAIL
import com.kcvn.spm.model.tables.references.PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.jooq.impl.DSL.*
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class OrderRepository(
    private val context: DSLContext,
    private val orderDetailRepository: OrderDetailRepository
) : SortingRepository() {
    fun getPaginatedOrder(
        request: OrderSearchRequest?,
        pageable: Pageable?
    ): Pair<List<OrderDetailModel>, Int> {
        var condition: Condition = DSL.noCondition()
        if (!request?.productName.isNullOrBlank()) {
            condition = condition.and(PRODUCT.NAME.eq(request?.productName))
        }
        if (!request?.frame_1.isNullOrBlank()) {
            condition = condition.and(PRODUCT.FRAME_1.eq(request?.frame_1))
        }
        if (!request?.srNosr.isNullOrBlank()) {
            condition = condition.and(PRODUCT.SR_NOSR.eq(request?.srNosr))
        }
        if (request?.startDate != null && request.endDate != null) {
            condition = condition.and(
                ORDER.START_DATE.greaterOrEqual(request.startDate)
                    .and(ORDER.END_DATE.lessOrEqual(request.endDate))
            )
        }
        if (!request?.orderCode.isNullOrBlank()) {
            condition = condition.and(ORDER.ORDER_CODE.eq(request?.orderCode))
        }
//        request?.version?.let { version ->
//            condition = condition.and(ORDER.VERSION.eq(version))
//        }
        val completionRateProcessesQuery = context.select(
            ORDER_DETAIL.ID,
            ORDER.QUANTITY,
            PRODUCT.FRAME_1,
            PRODUCT.LAYER_COUNT,
            PRODUCT.PCS_SH,
            PRODUCT.SH_BLOCK,
            PRODUCT.SR_NOSR,
            ORDER.VERSION,
            PRODUCT.NAME.`as`("productName"),
            substring(PRODUCT.NAME, 6, 10).`as`("productShortcutName"),
            ORDER.ID.`as`("orderId"),
            ORDER_DETAIL.PRODUCT_ID.`as`("productId")
        )
            .from(
                ORDER.join(ORDER_DETAIL).on(ORDER.ID.eq(ORDER_DETAIL.ORDER_ID))
                    .join(PRODUCT).on(ORDER_DETAIL.PRODUCT_ID.eq(PRODUCT.ID))
            )
            .where(
                condition.and(ORDER_DETAIL.IS_DELETED.eq(false))
                    .and(PRODUCT.IS_DELETED.eq(false))
                    .and(ORDER.IS_DELETED.eq(false))
            )
            .orderBy(getSortFields(pageable?.sort, ORDER.ORDER_CODE))
            .limit(pageable?.pageSize ?: 10)
            .offset(pageable?.offset ?: 0)
            .fetchInto(OrderDetailModel::class.java)
        val uniqueOrderProductPairs = completionRateProcessesQuery
            .distinctBy { it.orderId to it.productId }
        val total = context.fetchCount(ORDER_DETAIL, ORDER_DETAIL.IS_DELETED.eq(false))
        return Pair(uniqueOrderProductPairs, total)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        return when (sortFieldName) {
            "id" -> ORDER.ID
            "createddate" -> ORDER.CREATED_DATE
            else -> throw IllegalArgumentException("Could not find table field: $sortFieldName")
        }
    }


    fun getOrderCode(year: String): List<OrderCodeResponse> {
        val orders = context.select(
            ORDER.ORDER_CODE,
            ORDER.VERSION,
            ORDER.START_DATE,
            ORDER.END_DATE
        )
            .from(ORDER)
            .where(
                ORDER.IS_DELETED.eq(false)
            )
            .fetch()

        // Tạo một Map để lưu trữ danh sách các VERSION cho mỗi ORDER_CODE
        val versionMap = mutableMapOf<String, MutableList<String>>()
        for (order in orders) {
            val orderCode = order[ORDER.ORDER_CODE]
            val version = order[ORDER.VERSION]
            if (orderCode != null) {
                versionMap.computeIfAbsent(orderCode) { mutableListOf() }.add(version.toString())
            }
        }

        // Tạo danh sách OrderCodeResponse và điền thông tin từ versionMap
        val orderCodeResponses = mutableListOf<OrderCodeResponse>()
        for ((orderCode, versions) in versionMap) {
            val dropdownResponses = versions.map { DropdownResponse(it, it.toString()) }
            orderCodeResponses.add(OrderCodeResponse(orderCode, orderCode, dropdownResponses))
        }

        return orderCodeResponses
    }

    fun getVersionByOrderCode(orderCode: String): List<DropdownResponse> {
        val versions = context.select(ORDER.VERSION)
            .from(ORDER)
            .where(
                ORDER.IS_DELETED.eq(false),
                ORDER.ORDER_CODE.eq(orderCode)
            )
            .fetch()
            .map { it[ORDER.VERSION] }

        return versions.map { DropdownResponse(it.toString(), it.toString()) }
    }

    fun addOrder(order: Order, orderDetails: List<OrderDetail>) {
        startTransaction()
        try {
            val orderInsert = context.insertInto(ORDER, ORDER.ORDER_CODE, ORDER.START_DATE, ORDER.END_DATE, ORDER.VERSION, ORDER.CREATED_BY)
                .values(order.orderCode, order.startDate, order.endDate, order.version, CommonUtils.loggedInUser() ?: "SYSTEM")
                .returningResult(ORDER).fetchInto(Order::class.java).firstOrNull()

            if (orderInsert != null) {
                for (orderDetail in orderDetails) {
                    context.insertInto(ORDER_DETAIL, ORDER_DETAIL.ORDER_ID, ORDER_DETAIL.PRODUCT_ID, ORDER_DETAIL.ORDER_DATE, ORDER_DETAIL.QUANTITY, ORDER_DETAIL.CREATED_BY)
                        .values(orderInsert.id, orderDetail.productId, orderDetail.orderDate, orderDetail.quantity, CommonUtils.loggedInUser() ?: "SYSTEM")
                        .returningResult(ORDER_DETAIL).fetchInto(OrderDetail::class.java).firstOrNull()
                }
            }
        }
        catch (e: Exception) {
            rollback()
        }
    }


    fun getOverlapOrderDate(startDate: OffsetDateTime, endDate: OffsetDateTime, orderCode: String) : Order? {
        var condition = DSL.noCondition()
        condition = condition.and(ORDER.ORDER_CODE.notEqual(orderCode)).and(ORDER.IS_DELETED.eq(false))
            .and(
                (ORDER.END_DATE.ge(startDate).and(ORDER.END_DATE.lt(endDate)))
                .or(ORDER.START_DATE.le(endDate).and(ORDER.END_DATE.gt(endDate)))
            )
        return context.selectFrom(ORDER).where(condition).fetchInto(Order::class.java).firstOrNull()
    }

    fun getByOrderCode(orderCode: String) : Order? {
        return context.selectFrom(ORDER).where(ORDER.ORDER_CODE.eq(orderCode)).and(ORDER.IS_DELETED.eq(false))
            .fetchInto(Order::class.java).firstOrNull()
    }

    fun getByOrderByCodeAndVersion(orderCode: String, version: Int) : Order? {
        return context.selectFrom(ORDER).where(ORDER.ORDER_CODE.eq(orderCode)).and(ORDER.IS_DELETED.eq(false).and(ORDER.VERSION.eq(version)))
            .fetchInto(Order::class.java).firstOrNull()
    }
}