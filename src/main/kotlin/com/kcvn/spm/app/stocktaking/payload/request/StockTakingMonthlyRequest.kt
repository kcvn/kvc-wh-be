package com.kcvn.spm.app.stocktaking.payload.request

import java.time.LocalDate

data class StockTakingMonthlyRequest(
    var yearNumber: Int? = null,
    var monthNumber: Int? = null,
    val inspectionDate: LocalDate? = null,
    var poNumber: String? = null,
    var isDifferentBacklog: Boolean? = null
)
