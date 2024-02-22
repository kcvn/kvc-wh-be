package com.kcvn.spm.app.inventoryproduct.completionrate.payload.response

import java.math.BigDecimal

class CompletionRateProcessResponse(
    val id: String?,
    val key: String?,
    val processCode: String?,
    val layerCode: String?,
    val rate: BigDecimal?,
    val processName: String?,
    val processNameJp:String?
)
