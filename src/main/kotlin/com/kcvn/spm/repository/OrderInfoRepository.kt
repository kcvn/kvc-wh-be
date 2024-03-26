package com.kcvn.spm.repository

import com.kcvn.spm.app.order.payload.model.OrderDetailByDateModel
import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.OrderVersion
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.pojos.OrderInfo
import com.kcvn.spm.model.tables.pojos.OrderVersionDropdown
import com.kcvn.spm.model.tables.references.ORDER_INFO
import com.kcvn.spm.model.tables.references.ORDER_VERSION_DROPDOWN
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository

@Repository
class OrderInfoRepository (private val context: DSLContext): SortingRepository() {

    fun getPagingListOrder(request: OrderSearchRequest, pageable: Pageable): Pair<List<OrderDetailModel>, Int> {
        var condition: Condition = DSL.noCondition()
        if (!request.productName.isNullOrEmpty()) {
            condition = condition.and(ORDER_INFO.PRODUCT_NAME.containsIgnoreCase(request.productName))
        }
        if (!request.frame_1.isNullOrEmpty()) {
            condition = condition.and(ORDER_INFO.FRAME_1.eq(request.frame_1))
        }
        if (!request.srNosr.isNullOrEmpty()) {
            condition = condition.and(ORDER_INFO.SR_NOSR.eq(request.srNosr))
        }
        if (request.startDate != null) {
            condition = condition.and(ORDER_INFO.ORDER_DATE.ge(request.startDate))
        }
        if (request.endDate != null) {
            condition = condition.and(ORDER_INFO.ORDER_DATE.le(request.endDate))
        }
        if (!request.version.isNullOrEmpty()) {
            if (request.version == OrderVersion.LATEST) {
                condition = condition.and(ORDER_INFO.IS_LATEST.eq(true))
            }
            else {
                val versions = request.version?.split(",")?.map { x -> x.toInt() }
                if (!versions.isNullOrEmpty())
                    condition = condition.and(ORDER_INFO.VERSION.`in`(versions))
            }

        }
        condition = condition.and(ORDER_INFO.IS_DELETED.eq(false))

        val sortFields = getSortFields(pageable.sort, ORDER_INFO.PRODUCT_NAME).toMutableList()
        val sortVersion = pageable.sort.find { x -> x.property == "version" }
        if (sortVersion == null) {
            sortFields.add(sortFields.size - 1, ORDER_INFO.VERSION.desc())
        }

        if (request.version == OrderVersion.LATEST) {
            val query = context.select(
                ORDER_INFO.PRODUCT_NAME,
                DSL.sum(ORDER_INFO.QUANTITY).`as`("quantity"),
                ORDER_INFO.FRAME_1,
                ORDER_INFO.LAYER_COUNT,
                ORDER_INFO.PCS_SH,
                ORDER_INFO.BLOCK_SH,
                ORDER_INFO.SR_NOSR
            ).from(ORDER_INFO)
                .where(condition)
                .groupBy(
                    ORDER_INFO.PRODUCT_NAME,
                    ORDER_INFO.FRAME_1,
                    ORDER_INFO.LAYER_COUNT,
                    ORDER_INFO.PCS_SH,
                    ORDER_INFO.BLOCK_SH,
                    ORDER_INFO.SR_NOSR
                )

            val count = query.count()
            val data = query
                .orderBy(sortFields)
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(OrderDetailModel::class.java)

            return Pair(data, count)
        }
        else {
            val query = context.select(
                ORDER_INFO.PRODUCT_NAME,
                DSL.sum(ORDER_INFO.QUANTITY).`as`("quantity"),
                ORDER_INFO.FRAME_1,
                ORDER_INFO.LAYER_COUNT,
                ORDER_INFO.PCS_SH,
                ORDER_INFO.BLOCK_SH,
                ORDER_INFO.SR_NOSR,
                ORDER_INFO.VERSION
            ).from(ORDER_INFO)
                .where(condition)
                .groupBy(
                    ORDER_INFO.PRODUCT_NAME,
                    ORDER_INFO.FRAME_1,
                    ORDER_INFO.LAYER_COUNT,
                    ORDER_INFO.PCS_SH,
                    ORDER_INFO.BLOCK_SH,
                    ORDER_INFO.SR_NOSR,
                    ORDER_INFO.VERSION
                )

            val count = query.count()
            val data = query
                .orderBy(sortFields)
                .limit(pageable.pageSize)
                .offset(pageable.offset)
                .fetchInto(OrderDetailModel::class.java)

            return Pair(data, count)
        }
    }

    fun getQuantityByCalendar(productVersions: List<Pair<String, String>>, isLatest: Boolean): List<OrderDetailByDateModel> {
        val productNames = productVersions.map { x -> x.first }
        val versions = productVersions.map { x -> x.second.toIntOrNull() }
        if (isLatest) {
            val data = context.selectFrom(ORDER_INFO).where(
                ORDER_INFO.PRODUCT_NAME.`in`(productNames).and(ORDER_INFO.IS_DELETED.eq(false))
            ).fetchInto(OrderInfo::class.java).map { x ->
                OrderDetailByDateModel(
                    x.productName,
                    x.version.toString(),
                    DateTimeHelper.toTimeZone7toString(x.orderDate!!, DateTimeFormat.yyyyMMdd),
                    x.quantity
                )
            }.groupBy { x -> Pair(x.productName, x.orderDate) }.map { x ->
                OrderDetailByDateModel(
                    x.key.first,
                    "",
                    x.key.second,
                    x.value.sumOf { m -> m.quantity ?: 0 }
                )
            }
            return data
        }
        else {
            val data = context.selectFrom(ORDER_INFO).where(
                ORDER_INFO.PRODUCT_NAME.`in`(productNames).and(ORDER_INFO.VERSION.`in`(versions))
                    .and(ORDER_INFO.IS_DELETED.eq(false))
            ).fetchInto(OrderInfo::class.java).map { x ->
                OrderDetailByDateModel(
                    x.productName,
                    x.version.toString(),
                    DateTimeHelper.toTimeZone7toString(x.orderDate!!, DateTimeFormat.yyyyMMdd),
                    x.quantity
                )
            }
            return data
        }
    }

    fun getOrderVersionDropdown(): List<OrderVersionDropdown> {
        return context.selectFrom(ORDER_VERSION_DROPDOWN)
            .where(ORDER_VERSION_DROPDOWN.IS_DELETED.eq(false))
            .orderBy(ORDER_VERSION_DROPDOWN.CREATED_DATE.asc())
            .fetchInto(OrderVersionDropdown::class.java)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        return when (fieldName) {
            "version" -> ORDER_INFO.VERSION
            "productname" -> ORDER_INFO.PRODUCT_NAME
            "frame_1" -> ORDER_INFO.FRAME_1
            "layercount" -> ORDER_INFO.LAYER_COUNT
            else -> ORDER_INFO.PRODUCT_NAME
        }
    }

}