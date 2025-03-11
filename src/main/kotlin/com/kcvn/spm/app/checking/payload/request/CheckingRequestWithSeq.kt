package com.kcvn.spm.app.checking.payload.request

import java.math.BigDecimal

data class CheckingRequestWithSeq(
    var poNumber: String? = null,
    var packageCode: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO,
    var seqNo : Int? = 1
)
