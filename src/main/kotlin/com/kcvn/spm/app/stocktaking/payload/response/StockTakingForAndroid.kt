package com.kcvn.spm.app.stocktaking.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class StockTakingForAndroid(
    var inspectionDate: LocalDate? = null,
    var poNumber: String? = null,
    var amoebaLocationCode: String? = null,
    var actualLocationCode: String? = null,
    var amoebaQty: BigDecimal? = null,
    var actualQty: BigDecimal? = null
)
