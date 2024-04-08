package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

data class CreatePlanRequest (
    var startDate: OffsetDateTime? = null,
    var endDate: OffsetDateTime? = null
)