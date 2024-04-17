package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

data class CreatePlanRequest (
    var productNames: String? = null,
    var planMonth: String,
    var inventoryDate: OffsetDateTime? = null,
    var description: String? = null,
    var replan: Boolean? = null
)