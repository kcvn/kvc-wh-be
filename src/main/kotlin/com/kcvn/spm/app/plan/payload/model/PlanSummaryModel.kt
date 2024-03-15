package com.kcvn.spm.app.plan.payload.model

data class PlanSummaryModel (
    var processName: String? = null,
    var processNameJp: String? = null,
    var processConvertCode: String? = null,
    var processSequence: Int? = null,
    var details: MutableList<PlanSummaryDetailModel>? = mutableListOf()
)