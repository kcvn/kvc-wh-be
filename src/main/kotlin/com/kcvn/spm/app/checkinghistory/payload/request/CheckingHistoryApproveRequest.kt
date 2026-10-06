package com.kcvn.spm.app.checkinghistory.payload.request

import java.math.BigDecimal
import java.time.LocalDate

data class CheckingHistoryApproveRequest(
    var invoiceNumber: String,
    var poNumber: String,
    var seqNo: Int,
)