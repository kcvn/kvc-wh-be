package com.kcvn.spm.app.stocktaking.payload.response

import java.math.BigDecimal

data class SapStockTakingResponse(
    var lotNo: String,
    var itemCode: String? = null,
    var productionGroup: String? = null,
    var storageLocation: String,
    var itemType: String? = null,
    var sapQty: BigDecimal,
    var systemQty: BigDecimal,
    var result: String,
)
