package com.kcvn.spm.app.stocktaking.payload.request

data class StockTakingMonthlyRequest(
    var yearNumber: Int? = null,
    var monthNumber: Int? = null,
    var poNumber: String? = null,
    var conditionQuery: String? = null
)
