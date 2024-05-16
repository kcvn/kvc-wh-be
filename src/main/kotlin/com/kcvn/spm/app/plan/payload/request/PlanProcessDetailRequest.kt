package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

class PlanProcessDetailRequest (
    var planProductId: List<String>? = null,
    var filterType: Int = 0,
    var startDate: OffsetDateTime? = null,
    var endDate: OffsetDateTime? = null,
    var orderCode: String? = ""
)