package com.kcvn.spm.app.transaction.sending.payload.request

import java.math.BigDecimal
import java.time.LocalDate

data class ImportSending(
    var inspectionDate: LocalDate? = null,
    var poNumber: String? = null,
    var qty: BigDecimal? = BigDecimal.ZERO,
    var formCode: String? = null
)
