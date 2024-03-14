package com.kcvn.spm.app.plan.payload.model

data class PlanSummaryModel (
    var frame_1: String? = null,
    var mold: String? = null,
    var processName: String? = null,
    var processNameJp: String? = null,
    var processConvertCode: String? = null,
    var processSequence: Int? = null,
    var details: MutableList<PlanSummaryDetailModel>? = mutableListOf()
)