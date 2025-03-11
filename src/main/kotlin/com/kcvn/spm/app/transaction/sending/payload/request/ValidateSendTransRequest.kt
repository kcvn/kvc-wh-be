package com.kcvn.spm.app.transaction.sending.payload.request

import java.math.BigDecimal

data class ValidateSendTransRequest(
    var sourceLocationCode: String? = null,
    var sourcePackageCode: String? = null,
    var poNumber: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO
)
