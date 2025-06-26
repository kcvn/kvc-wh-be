package com.kcvn.spm.app.checkinghistory.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class CheckingHistoryResponse(
    var scanDate: LocalDate? = null,
    var poNumber: String? = null,
    var importQty: BigDecimal? = null,
    var scanQty: BigDecimal? = null,
    var seqNo: Int? = null,
    var formCode: String? = null,
    var result: String? = null
)