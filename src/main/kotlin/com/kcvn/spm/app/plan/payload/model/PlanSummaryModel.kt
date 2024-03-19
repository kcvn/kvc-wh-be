package com.kcvn.spm.app.plan.payload.model

data class PlanSummaryModel (
    var processName: String? = null,
    var processNameJp: String? = null,
    var processConvertCode: String? = null,
    var processSequence: Int? = null,
    var details: MutableList<PlanSummaryDetailModel>? = mutableListOf(),

    var frame1: String? =null,
    var processCode: String? =null,
    var unit: String?=null
)