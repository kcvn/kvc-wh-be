package com.kcvn.spm.app.cancel.moving.payload.response

import java.math.BigDecimal

data class CancelMovingResponse(
    var sourceLocationCode: String? = null,
    var destLocationCode: String? = null,
    var sourcePackageCode: String? = null,
    var destPackageCode: String? = null,
    var poNumber: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO,
    var seqNo : Int? = 1
)
