package com.kcvn.spm.app.checkinghistory.payload.response

import java.math.BigDecimal

data class CheckingHistorySummaryResponse(
    var totalPo: Int = 0,
    var totalInvoice: Int = 0,
    var totalOrderQty: BigDecimal = BigDecimal.ZERO,
)
