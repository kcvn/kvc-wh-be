package com.kcvn.spm.app.cancel.receiving.payload.response

import java.math.BigDecimal

data class CancelRecTransResponse(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO,
    var seqNo : Int? = 1
)
