package com.kcvn.spm.app.plan.payload.model


data class EquipmentProductivityModel(
    var frame1: String,
    var processName: String,
    var processNameJp: String,
    var processConvertCode: String,
    var processDetail: List<ProcessDetailModel>
)


