package com.kcvn.spm.repository

import com.kcvn.spm.common.payload.KeyValueResponse
import com.kcvn.spm.model.tables.references.ORDER_DETAIL
import org.jooq.DSLContext
import org.jooq.Record2
import org.springframework.stereotype.Repository
import java.time.format.DateTimeFormatter

@Repository
class OrderDetailRepository (private val context: DSLContext) {


    fun GetCalenderOrderDetailByOrder(orderId: String): List<KeyValueResponse> {
        val result = context.select(
            ORDER_DETAIL.ORDER_DATE,
            ORDER_DETAIL.QUANTITY
        )
            .from(ORDER_DETAIL)
            .where(ORDER_DETAIL.ORDER_ID.eq(orderId))
            .fetch()

        return result.map { record ->
            KeyValueResponse(
                key = record[ORDER_DETAIL.ORDER_DATE]?.format(DateTimeFormatter.ofPattern("MM/dd/yyyy")),
                value = record[ORDER_DETAIL.QUANTITY].toString()
            )
        }
    }
}