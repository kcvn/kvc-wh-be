package com.kcvn.spm.app.stocktaking.payload.request

data class SapStockTakingSearchRequest (
    var lotNo: String? = null,
    var conditionQuery: String? = null
)