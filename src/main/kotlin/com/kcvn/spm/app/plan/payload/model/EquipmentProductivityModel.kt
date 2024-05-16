package com.kcvn.spm.app.plan.payload.model


data class EquipmentProductivityModel(
    var frame1: String? =null,
    var processName: String? =null,
    var processNameJp: String? =null,
    var processConvertCode: String? =null,
    var processCode: String? =null,
    var unit: String?=null,
    var processDetail: MutableList<ProcessDetailModel>? = mutableListOf()
)


