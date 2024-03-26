package com.kcvn.spm.app.order.payload.response

import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.CalendarResponse

data class PagingOrderResponse(
    var columns: List<CalendarResponse> = listOf()
) : BasePagingResponse<OrderDetailModel>()
