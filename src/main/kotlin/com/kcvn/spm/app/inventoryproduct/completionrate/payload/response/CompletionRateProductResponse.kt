package com.kcvn.spm.app.inventoryproduct.completionrate.payload.response

import java.math.BigDecimal

data class CompletionRateProductResponse(
    val id: String?,
    val productName: String?,
    val rate: BigDecimal?
)
