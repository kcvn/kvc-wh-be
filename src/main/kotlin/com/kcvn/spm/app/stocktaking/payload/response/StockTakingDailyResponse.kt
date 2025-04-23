package com.kcvn.spm.app.stocktaking.payload.response

import java.math.BigDecimal

data class StockTakingDailyResponse(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var amoebaQty: BigDecimal? = null,
    var systemQty: BigDecimal? = null,
    var result: String? = null
)
