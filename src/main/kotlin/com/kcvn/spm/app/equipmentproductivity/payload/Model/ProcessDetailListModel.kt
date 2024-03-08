package com.kcvn.spm.app.equipmentproductivity.payload.Model

import com.kcvn.spm.app.order.payload.response.CalendarValueResponse

data class ProcessDetailListModel(
    var type: String,
    var quantityByCalendars: List<CalendarValueResponse>
)