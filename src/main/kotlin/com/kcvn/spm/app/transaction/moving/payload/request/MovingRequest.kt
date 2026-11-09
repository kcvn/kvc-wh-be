package com.kcvn.spm.app.transaction.moving.payload.request

import java.math.BigDecimal

data class MovingRequest(
    var destLocationCode: String? = null,
    var sourcePackageCode: String? = null,
)
