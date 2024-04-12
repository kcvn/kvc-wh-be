package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

data class CreatePlanRequest (
    var month: Int,
    var year: Int,
    var startDate: OffsetDateTime? = null,
    var endDate: OffsetDateTime? = null,
    var inventoryDate: OffsetDateTime? = null,
    var description: String? = null,
    var isRePlan: Boolean? = null
)