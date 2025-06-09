package com.kcvn.spm.app.checkinghistory.payload.request

import java.math.BigDecimal

data class CheckingHistoryRequest(
    var reChecking: Boolean,
    var poNumber: String,
    var importQty : BigDecimal,
    var scanQty : BigDecimal
)