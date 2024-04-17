package com.kcvn.spm.repository

import com.kcvn.spm.app.order.payload.model.OrderDetailByDateModel
import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.app.report.externalquality.payload.model.ExternalQualityReportModel
import com.kcvn.spm.app.report.externalquality.payload.request.ExternalQualityReportSearchRequest
import com.kcvn.spm.common.constants.Constants
import com.kcvn.spm.common.constants.DateTimeFormat
import com.kcvn.spm.common.constants.OrderVersion
import com.kcvn.spm.common.helper.DateTimeHelper
import com.kcvn.spm.common.helper.StringHelper
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.common.util.CommonUtils
import com.kcvn.spm.model.tables.pojos.OrderInfo
import com.kcvn.spm.model.tables.pojos.OrderVersionDropdown
import com.kcvn.spm.model.tables.references.ORDER_INFO
import com.kcvn.spm.model.tables.references.ORDER_VERSION_DROPDOWN
import com.kcvn.spm.model.tables.references.PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import java.time.OffsetDateTime

@Repository
class OrderInfoRepository(private val context: DSLContext) : SortingRepository() {

    fun getListOrderForReport(request: ExternalQualityReportSearchRequest, pageable: Pageable): Pair<List<ExternalQualityReportModel>, Int> {
        var condition: Condition = DSL.noCondition()
        if (!request.productName.isNullOrEmpty()) {
            condition = condition.and(ORDER_INFO.PRODUCT_NAME.containsIgnoreCase(request.productName))
        }
        if (!request.mold.isNullOrEmpty()) {
            condition = condition.and(PRODUCT.MOLD.eq(request.mold))
        }

        if (!request.tapeCommon.isNullOrEmpty()) {
            condition = condition.and(PRODUCT.TAPE_COMMON.eq(request.tapeCommon))
        }
        if(request.startDate !=null ){
            condition = condition.and(ORDER_INFO.ORDER_DATE.ge(DateTimeHelper.toTimeZone7(request.startDate)))
        }
        if(request.endDate !=null ){
            condition = condition.and(ORDER_INFO.ORDER_DATE.le(DateTimeHelper.toTimeZone7(request.endDate)))
        }
        condition = condition.and(ORDER_INFO.IS_DELETED.eq(false)).and(ORDER_INFO.IS_LATEST.eq(true))
        val sortFields = getSortFields(pageable.sort, ORDER_INFO.PRODUCT_NAME).toMutableList()

        val query = context.select(
            ORDER_INFO.PRODUCT_NAME.`as`("productName"),
            DSL.right(ORDER_INFO.PRODUCT_NAME, 7).`as`("productShortcutName"),
            PRODUCT.MOLD.`as`("mold"),
            ORDER_INFO.PCS_SH.`as`("pcsSh"),
            ORDER_INFO.BLOCK_SH.`as`("blockSh"),
            PRODUCT.SNAP_MOLD.`as`("snapMold"),
            ORDER_INFO.LAYER_COUNT.`as`("layerCount"),
            PRODUCT.TAPE_COMMON.`as`("tapeCommon"),
            PRODUCT.PRODUCT_LINE.`as`("productLine"),
            PRODUCT.EXPORT_TYPE.`as`("exportType"),
            ).from(ORDER_INFO)
            .join(PRODUCT)
            .on(ORDER_INFO.PRODUCT_NAME.eq(PRODUCT.NAME).and(PRODUCT.IS_DELETED.eq(false)))
            .where(condition)
            .groupBy(
                ORDER_INFO.PRODUCT_NAME,
                ORDER_INFO.LAYER_COUNT,
                ORDER_INFO.PCS_SH,
                ORDER_INFO.BLOCK_SH,
                PRODUCT.TAPE_COMMON,
                PRODUCT.SNAP_MOLD,
                PRODUCT.EXPORT_TYPE,
                PRODUCT.MOLD,
                PRODUCT.PRODUCT_LINE
            )

        val count = query.count()
        val data = query.orderBy(sortFields).limit(pageable.pageSize).offset(pageable.offset)
            .fetchInto(ExternalQualityReportModel::class.java)
        return Pair(data, count)

    }

    fun getPagingListOrder(request: OrderSearchRequest, pageable: Pageable, isExport: Boolean = false): Pair<List<OrderDetailModel>, Int> {
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
            } else {
                val versions = request.version?.split(",")?.mapNotNull { x -> x.toIntOrNull() }
                if (!versions.isNullOrEmpty())
                    condition = condition.and(ORDER_INFO.VERSION.`in`(versions))
            }
        }
        condition = condition.and(ORDER_INFO.IS_DELETED.eq(false))

        val sortFields = getSortFields(pageable.sort, ORDER_INFO.PRODUCT_NAME).toMutableList()

        if (request.version == OrderVersion.LATEST) {
            val query = context.select(
                ORDER_INFO.PRODUCT_NAME,
                DSL.sum(ORDER_INFO.QUANTITY).`as`("quantity"),
                ORDER_INFO.FRAME_1,
                ORDER_INFO.LAYER_COUNT,
                ORDER_INFO.PCS_SH,
                ORDER_INFO.BLOCK_SH.`as`("shBlock"),
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

            if (isExport) {
                val data = query.orderBy(sortFields).fetchInto(OrderDetailModel::class.java)
                return Pair(data, data.size)
            } else {
                val count = query.count()
                val data = query.orderBy(sortFields).limit(pageable.pageSize).offset(pageable.offset)
                    .fetchInto(OrderDetailModel::class.java)
                return Pair(data, count)
            }

        } else {
            val sortFieldIndex = if (sortFields.isNotEmpty()) sortFields.size - 1 else 0
            sortFields.add(sortFieldIndex, ORDER_INFO.VERSION.desc())
            val query = context.select(
                ORDER_INFO.PRODUCT_NAME,
                DSL.sum(ORDER_INFO.QUANTITY).`as`("quantity"),
                ORDER_INFO.FRAME_1,
                ORDER_INFO.LAYER_COUNT,
                ORDER_INFO.PCS_SH,
                ORDER_INFO.BLOCK_SH.`as`("shBlock"),
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

            if (isExport) {
                val data = query.orderBy(sortFields).fetchInto(OrderDetailModel::class.java)
                return Pair(data, data.size)
            } else {
                val count = query.count()
                val data = query.orderBy(sortFields).limit(pageable.pageSize).offset(pageable.offset)
                    .fetchInto(OrderDetailModel::class.java)
                return Pair(data, count)
            }
        }
    }

    fun getQuantityByCalendar(
        productVersions: List<Pair<String, String>>,
        startDate: OffsetDateTime?,
        endDate: OffsetDateTime?,
        isLatest: Boolean
    ): List<OrderDetailByDateModel> {
        val productNames = productVersions.map { x -> x.first }
        val versions = productVersions.map { x -> x.second.toIntOrNull() }
        var condition = DSL.noCondition().and(ORDER_INFO.PRODUCT_NAME.`in`(productNames))
            .and(ORDER_INFO.IS_DELETED.eq(false))
        if (startDate != null) {
            condition = condition.and(ORDER_INFO.ORDER_DATE.ge(startDate))
        }
        if (endDate != null) {
            condition = condition.and(ORDER_INFO.ORDER_DATE.le(endDate))
        }
        if (isLatest) {
            var data = context.selectFrom(ORDER_INFO)
                .where(condition.and(ORDER_INFO.IS_LATEST.eq(true)))
                .fetchInto(OrderInfo::class.java).map { x ->
                    OrderDetailByDateModel(
                        x.productName,
                        x.version.toString(),
                        DateTimeHelper.toTimeZone7toString(x.orderDate!!, DateTimeFormat.yyyyMMdd),
                        x.quantity
                    )
                }

            val versionByProducts = data.groupBy { x -> x.productName }.map { x ->
                Pair(
                    x.key,
                    x.value.sortedWith(compareBy<OrderDetailByDateModel> { m -> m.orderDate }.thenByDescending { m -> m.version }).firstOrNull()?.version ?: ""
                )
            }

            data = data.groupBy { x -> Pair(x.productName, x.orderDate) }.map { x ->
                OrderDetailByDateModel(
                    x.key.first,
                    versionByProducts.find { m -> m.first == x.key.first }?.second,
                    x.key.second,
                    x.value.sumOf { m -> m.quantity ?: 0 }
                )
            }
            return data
        } else {
            val data = context.selectFrom(ORDER_INFO)
                .where(condition.and(ORDER_INFO.VERSION.`in`(versions)))
                .fetchInto(OrderInfo::class.java).map { x ->
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

    fun getProductNameByOder(startDate: OffsetDateTime?, endDate: OffsetDateTime?): List<OrderInfo?> {
        return context
            .selectFrom(ORDER_INFO)
            .where(ORDER_INFO.ORDER_DATE.between(startDate, endDate)
                .and(ORDER_INFO.IS_LATEST.eq(true))
                .and(ORDER_INFO.IS_DELETED.eq(false)))
            .fetchInto(OrderInfo::class.java)
    }

    fun getOrderVersionDropdown(): List<OrderVersionDropdown> {
        return context.selectFrom(ORDER_VERSION_DROPDOWN)
            .where(ORDER_VERSION_DROPDOWN.IS_DELETED.eq(false))
            .orderBy(ORDER_VERSION_DROPDOWN.CREATED_DATE.asc())
            .fetchInto(OrderVersionDropdown::class.java)
    }

    fun addOrderInfo(orders: List<OrderInfo>, isIncreaseVersion: Boolean, startDate: OffsetDateTime, endDate: OffsetDateTime) {
        context.transaction { configuration ->
            val transactionalContext = DSL.using(configuration)
            val productNames = orders.map { x -> x.productName }.distinct()
            val orderExists = transactionalContext.selectFrom(ORDER_INFO)
                .where(ORDER_INFO.PRODUCT_NAME.`in`(productNames)).and(ORDER_INFO.IS_DELETED.eq(false))
                .and(ORDER_INFO.ORDER_DATE.ge(startDate)).and(ORDER_INFO.ORDER_DATE.le(endDate))
                .fetchInto(OrderInfo::class.java)

            var lstVersion = mutableListOf<Int>()
            val orderIds = orderExists.filter { x ->
                orders.any { m -> m.orderDate != null && m.productName == x.productName && m.orderDate!!.isEqual(x.orderDate) }
            }.map { x -> x.id }
            if (isIncreaseVersion) {
                transactionalContext.update(ORDER_INFO)
                    .set(ORDER_INFO.IS_LATEST, false)
                    .where(ORDER_INFO.ID.`in`(orderIds))
                    .execute()
            } else {
                transactionalContext.deleteFrom(ORDER_INFO)
                    .where(ORDER_INFO.ID.`in`(orderIds))
                    .execute()
            }
            val query = orders.map { item ->
                val order = orderExists.filter { x -> x.orderDate != null && x.productName == item.productName && x.orderDate!!.isEqual(item.orderDate) }
                    .sortedByDescending { x -> x.version }.firstOrNull()
                var version = order?.version ?: 0
                if (isIncreaseVersion && order != null) {
                    version += 1
                }
                lstVersion.add(version)
                transactionalContext.insertInto(
                    ORDER_INFO,
                    ORDER_INFO.PRODUCT_NAME,
                    ORDER_INFO.FRAME_1,
                    ORDER_INFO.LAYER_COUNT,
                    ORDER_INFO.PCS_SH,
                    ORDER_INFO.BLOCK_SH,
                    ORDER_INFO.SR_NOSR,
                    ORDER_INFO.VERSION,
                    ORDER_INFO.ORDER_DATE,
                    ORDER_INFO.QUANTITY,
                    ORDER_INFO.IS_LATEST,
                    ORDER_INFO.CREATED_BY
                ).values(
                    item.productName,
                    item.frame_1,
                    item.layerCount,
                    item.pcsSh,
                    item.blockSh,
                    item.srNosr,
                    version,
                    item.orderDate,
                    item.quantity,
                    true,
                    CommonUtils.loggedInUser() ?: Constants.SYSTEM
                )
            }
            transactionalContext.batch(query).execute()

            val orderVersion = transactionalContext.selectFrom(ORDER_VERSION_DROPDOWN)
                .where(ORDER_VERSION_DROPDOWN.IS_DELETED.eq(false))
                .fetchInto(OrderVersionDropdown::class.java)

            lstVersion = lstVersion.filter { x -> !orderVersion.any { m -> m.version == x.toString() } }.distinct().toMutableList()
            val versionQuery = lstVersion.map { item ->
                transactionalContext.insertInto(
                    ORDER_VERSION_DROPDOWN,
                    ORDER_VERSION_DROPDOWN.VERSION,
                    ORDER_VERSION_DROPDOWN.LABEL,
                    ORDER_VERSION_DROPDOWN.CREATED_BY
                ).values(
                    item.toString(),
                    "V${StringHelper.intToStringD2(item)}",
                    CommonUtils.loggedInUser() ?: Constants.SYSTEM
                )
            }
            transactionalContext.batch(versionQuery).execute()
        }
    }

    fun getOrderInfoByTimeRange(startDate: OffsetDateTime?, endDate: OffsetDateTime?): List<OrderInfo> {
        return context.selectFrom(ORDER_INFO)
            .where(ORDER_INFO.ORDER_DATE.between(startDate, endDate)
                .and(ORDER_INFO.IS_LATEST.eq(true))
                .and(ORDER_INFO.IS_DELETED.eq(false)))
            .fetchInto(OrderInfo::class.java)
    }

    override fun getTableField(sortFieldName: String): TableField<*, *> {
        val fieldName = sortFieldName.lowercase()
        return when (fieldName) {
            "productname" -> ORDER_INFO.PRODUCT_NAME
            "frame_1" -> ORDER_INFO.FRAME_1
            "layercount" -> ORDER_INFO.LAYER_COUNT
            else -> ORDER_INFO.PRODUCT_NAME
        }
    }

}