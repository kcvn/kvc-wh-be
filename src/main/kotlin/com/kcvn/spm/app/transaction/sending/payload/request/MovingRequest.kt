package com.kcvn.spm.app.transaction.sending.payload.request

import java.math.BigDecimal

data class MovingRequest(
    var sourceLocationCode: String? = null,
    var destLocationCode: String? = null,
    var sourcePackageCode: String? = null,
    var destPackageCode: String? = null,
    var poNumber: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO
)
