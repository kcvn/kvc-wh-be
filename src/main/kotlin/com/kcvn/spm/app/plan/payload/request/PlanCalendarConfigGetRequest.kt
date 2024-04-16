package com.kcvn.spm.app.plan.payload.request

data class PlanCalendarConfigGetRequest(
    var planMonth: String,
    var hasDefault: Boolean? = null
)