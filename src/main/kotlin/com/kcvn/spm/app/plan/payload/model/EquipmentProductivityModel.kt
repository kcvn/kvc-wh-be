package com.kcvn.spm.app.plan.payload.model


data class EquipmentProductivityModel(
    var frame1: String? =null,
    var processName: String? =null,
    var processNameJp: String? =null,
    var processConvertCode: String? =null,
    var processDetail: List<ProcessDetailModel>? = listOf()
)


