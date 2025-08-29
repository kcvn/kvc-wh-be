package com.kcvn.spm.app.stocktaking.payload.response

import java.math.BigDecimal

data class StockTakingMonthlyResponse(
    var poNumber: String? = null,
    var packageCode: String? = null,
    var systemLocationCode: String? = null,
    var actualLocationCode: String? = null,
    var checkingLocationCode: String? = null,
    var systemQty: BigDecimal? = null,
    var actualQty: BigDecimal? = null,
    var checkingQty: BigDecimal? = null,
    var systemBoxQty: Int? = null,
    var actualBoxQty: Int? = null,
    var checkingBoxQty: Int? = null,
    var resultLocationCode: String? = null,
    var resultQty: String? = null,
    var resultBoxQty: String? = null,
)
