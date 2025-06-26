package com.kcvn.spm.app.tempcheckingimported.payload.response

import java.math.BigDecimal

data class TempCheckingImportedResponse(
    var poNumber: String? = null,
    var qty: BigDecimal? = null,
    var formCode: String? = null
)
