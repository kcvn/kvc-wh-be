package com.kcvn.spm.repository

import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.app.order.payload.request.OrderSearchRequest
import com.kcvn.spm.common.repository.SortingRepository
import com.kcvn.spm.model.tables.references.ORDER
import com.kcvn.spm.model.tables.references.ORDER_DETAIL
import com.kcvn.spm.model.tables.references.PRODUCT
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.TableField
import org.jooq.impl.DSL
import org.springframework.data.domain.Pageable
import org.springframework.stereotype.Repository
import org.jooq.impl.DSL.substring
@Repository
class OrderRepository (private val context: DSLContext,
                       private val orderDetailRepository: OrderDetailRepository) : SortingRepository(){

    fun getPaginatedCompletionRateProduct(
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
//        request?.year?.let { year ->
//            condition = condition.and(DSL.year(ORDER.START_DATE.).eq(year))
//        }
        if (!request?.orderCode.isNullOrBlank()) {
            condition = condition.and(ORDER.ORDER_CODE.eq(request?.orderCode))
        }
        request?.version?.let { version ->
            condition = condition.and(ORDER.VERSION.eq(version))
        }
        val completionRateProcessesQuery = context.select(
            ORDER.ID,
            ORDER.QUANTITY,
            PRODUCT.FRAME_1,
            PRODUCT.LAYER_COUNT,
            PRODUCT.PCS_SH,
            PRODUCT.SH_BLOCK,
            PRODUCT.SR_NOSR,
            ORDER.VERSION,
            PRODUCT.NAME.`as`("productName"),
            substring(PRODUCT.NAME,6,10).`as`("productShortcutName")

        )
            .from(ORDER.join(ORDER_DETAIL).on(ORDER.ID.eq(ORDER_DETAIL.ORDER_ID))
                .join(PRODUCT).on(ORDER_DETAIL.PRODUCT_ID.eq(PRODUCT.ID)))

            .where(
                condition.and(ORDER_DETAIL.IS_DELETED.eq(false))
                    .and(PRODUCT.IS_DELETED.eq(false))
                    .and(ORDER.IS_DELETED.eq(false))
            )
            .orderBy(getSortFields(pageable?.sort, ORDER.ORDER_CODE))
            .limit(pageable?.pageSize ?: 10)
            .offset(pageable?.offset ?: 0)
            .fetchInto(OrderDetailModel::class.java)
        val additionalData = completionRateProcessesQuery.map { orderDetailModel ->
            orderDetailRepository.GetCalenderOrderDetailByOrder(orderDetailModel.id)
        }
        completionRateProcessesQuery.forEachIndexed { index, orderDetailModel ->
            orderDetailModel.quantityByCalendars = additionalData[index]
        }
        val total = context.fetchCount(ORDER, ORDER.IS_DELETED.eq(false))
        return Pair(completionRateProcessesQuery, total)
    }



    override fun getTableField(sortFieldName: String): TableField<*, *> {
        return when (sortFieldName) {
            "id" -> ORDER.ID
            "createddate" -> ORDER.CREATED_DATE
            else -> throw IllegalArgumentException("Could not find table field: $sortFieldName")
        }
    }


}