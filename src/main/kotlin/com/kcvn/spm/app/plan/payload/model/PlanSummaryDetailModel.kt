package com.kcvn.spm.app.plan.payload.model

data class PlanSummaryDetailModel (
    var type: String? = null,
    var planSummaryData: List<PlanDataByProcessModel>? = listOf()
)