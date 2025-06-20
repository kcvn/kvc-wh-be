package com.kcvn.spm.app.stocktaking.payload.request

import java.math.BigDecimal

data class ScanRequest(
    var packageCode: String? = null,
    var actualLocationCode: String? = null,
    var actualQty : BigDecimal? = BigDecimal.ZERO,
    var actualBoxQty : Int? = 0
)
