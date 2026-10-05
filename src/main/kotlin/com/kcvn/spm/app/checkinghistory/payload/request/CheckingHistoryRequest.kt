package com.kcvn.spm.app.checkinghistory.payload.request

import java.math.BigDecimal
import java.time.LocalDate

data class CheckingHistoryRequest(
    var reChecking: Boolean,
    var invoiceNumber: String,
    var poNumber: String,
    var qty : BigDecimal,
    var lotNo: String?,
    var receivingDate: LocalDate?,
    var specifyInvoice: Boolean,
)