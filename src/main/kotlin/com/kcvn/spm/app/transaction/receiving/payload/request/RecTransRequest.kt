package com.kcvn.spm.app.transaction.receiving.payload.request

import java.math.BigDecimal

data class RecTransRequest(
    var locationCode: String? = null,
    var packageCode: String? = null,
    var poNumber: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO
)
