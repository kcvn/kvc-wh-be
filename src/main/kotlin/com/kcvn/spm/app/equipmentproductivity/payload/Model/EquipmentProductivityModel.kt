package com.kcvn.spm.app.equipmentproductivity.payload.Model

import com.kcvn.spm.app.order.payload.response.CalendarValueResponse


data class EquipmentProductivityModel(
    var frame1: String,
    var processName: String,
    var processNameJp: String,
    var processConvertCode: String,
    var processDetail: List<ProcessDetailModel>
)


