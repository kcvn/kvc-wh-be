package com.kcvn.spm.app.stocktaking.payload.request

import java.time.LocalDate

data class StockTakingDailyRequest (
    val inspectionDate: LocalDate? = null,
    var poNumber: String? = null,
    var conditionQuery: String? = null
)