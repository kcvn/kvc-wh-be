package com.kcvn.spm.app.completionrate.payload.response

import java.math.BigDecimal
import java.time.LocalDateTime

class CompletionRateProcessResponse(
        val id: String?,
        val key: String?,
        val processCode: String?,
        val layerCode: String?,
        val rate: BigDecimal?,
)
