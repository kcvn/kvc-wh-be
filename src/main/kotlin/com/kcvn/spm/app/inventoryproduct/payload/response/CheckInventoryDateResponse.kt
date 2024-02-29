package com.kcvn.spm.app.inventoryproduct.payload.response

import java.time.OffsetDateTime

data class CheckInventoryDateResponse (
    var hasInventoryDate: Boolean = false,
    var inventorydate: OffsetDateTime? = null
)
