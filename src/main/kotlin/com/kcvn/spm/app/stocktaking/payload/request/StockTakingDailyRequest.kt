package com.kcvn.spm.app.stocktaking.payload.request

data class StockTakingDailyRequest (
    var locationCode: String? = null,
    var poNumber: String? = null,
    var isDifferentBacklog: Boolean? = null
)