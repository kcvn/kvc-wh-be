package com.kcvn.spm.app.stocktaking.payload.request

import java.math.BigDecimal
import java.time.LocalDate

data class ScanRequest(
    var inspectionDate: LocalDate? = null,
    var poNumber: String? = null,
    var actualLocationCode: String? = null,
    var actualQty : BigDecimal? = BigDecimal.ZERO
)
