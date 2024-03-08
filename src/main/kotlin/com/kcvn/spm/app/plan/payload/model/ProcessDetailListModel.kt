package com.kcvn.spm.app.plan.payload.model

import com.kcvn.spm.app.order.payload.response.CalendarValueResponse

data class ProcessDetailListModel(
    var type: String,
    var quantityByCalendars: List<CalendarValueResponse>
)