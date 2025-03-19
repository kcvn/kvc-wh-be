package com.kcvn.spm.app.transaction.moving.payload.request

import java.math.BigDecimal

data class MovingRequest(
    var destLocationCode: String? = null,
    var destPackageCode: String? = null,
    var sourceLocationCode: String? = null,
    var sourcePackageCode: String? = null,
    var poNumber: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO,
    var boxQty : Int? = 0,
)
