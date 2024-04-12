package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

data class PlanCalendarConfigGetRequest(
    var date: OffsetDateTime,
    var checkVersion: Boolean? = null
)