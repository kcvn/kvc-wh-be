package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

data class PlanCalendarConfigUpdateRequest(
    var month: OffsetDateTime,
    var startDate: OffsetDateTime,
    var endDate: OffsetDateTime
)