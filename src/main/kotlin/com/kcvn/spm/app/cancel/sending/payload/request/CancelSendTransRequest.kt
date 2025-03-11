package com.kcvn.spm.app.cancel.sending.payload.request

import java.math.BigDecimal

data class CancelSendTransRequest(
    var locationCode: String? = null,
    var packageCode: String? = null,
    var poNumber: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO,
    var seqNo : Int? = 1
)
