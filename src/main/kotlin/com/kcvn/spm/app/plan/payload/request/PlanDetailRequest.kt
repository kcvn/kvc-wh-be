package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

data class PlanDetailRequest (
    var planProductId: String = "",
    var filterType: Int = 0,
    var startDate: OffsetDateTime? = null,
    var endDate: OffsetDateTime? = null,
    var orderCode: String = ""
)