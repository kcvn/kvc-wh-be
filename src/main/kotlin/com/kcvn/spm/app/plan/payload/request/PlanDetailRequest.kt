package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

data class PlanDetailRequest (
    var productName: String = "",
    var startDate: OffsetDateTime? = null,
    var endDate: OffsetDateTime? = null,
    var inventoryWorkPlan: Boolean? = null,
    var draftWorkPlan: Boolean? = null,
    var processGroups: String? = null
)