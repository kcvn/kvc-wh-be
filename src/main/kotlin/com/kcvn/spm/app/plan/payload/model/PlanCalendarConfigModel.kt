package com.kcvn.spm.app.plan.payload.model

import java.time.OffsetDateTime

data class PlanCalendarConfigModel (
    var month: Int? = null,
    var year: Int? = null,
    var startDate: OffsetDateTime? = null,
    var endDate: OffsetDateTime? = null,
    var currentPlanVersion: String? = null
)