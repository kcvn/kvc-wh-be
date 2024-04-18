package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

data class CheckInventoryRequest (
    var inventoryDate: OffsetDateTime? = null
)