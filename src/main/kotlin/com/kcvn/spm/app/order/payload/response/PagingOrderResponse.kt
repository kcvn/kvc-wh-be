package com.kcvn.spm.app.order.payload.response

import com.kcvn.spm.app.order.payload.model.OrderDetailModel
import com.kcvn.spm.common.payload.BasePagingResponse

data class PagingOrderResponse(
    var columns: List<CalendarValueResponse>? = null
) : BasePagingResponse<OrderDetailModel>()
