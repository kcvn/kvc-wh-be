package com.kcvn.spm.app.checking.payload.request

import java.math.BigDecimal

data class CheckingRequest(
    var poNumber: String? = null,
    var packageCode: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO,
    var lotNo: String? = null,
    var issueDate: String? = null,
)
