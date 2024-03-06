package com.kcvn.spm.app.plan.payload.model

import com.kcvn.spm.common.payload.KeyValueResponse

data class PlanDataByProcessModel(
    var title: String? = null,
    var inventory: Int? = null,
    var sumInventory: Int? = null,
    var quantityByCalendars: List<KeyValueResponse>? = listOf()
)
