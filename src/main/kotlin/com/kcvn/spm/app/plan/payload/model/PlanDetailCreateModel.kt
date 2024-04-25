package com.kcvn.spm.app.plan.payload.model

import java.time.OffsetDateTime

data class PlanDetailCreateModel(
    var title: String? = null,
    var planDate: OffsetDateTime? = null,
    var sheetQuantity: Int? = null,
    var blockQuantity: Int? = null,
    var orderDate: OffsetDateTime? = null,
    var hasInventory: Boolean? = null
)
