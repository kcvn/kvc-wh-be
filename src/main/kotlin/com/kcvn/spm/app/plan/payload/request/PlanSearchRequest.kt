package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

data class PlanSearchRequest(
    var productName: String? = null,
    var frame_1: String? = null,
    var mold: String? = null,
    var filterType: Int = 0,
    var startDate: OffsetDateTime? = null,
    var endDate: OffsetDateTime? = null,
    var orderCode: String? = null
)
