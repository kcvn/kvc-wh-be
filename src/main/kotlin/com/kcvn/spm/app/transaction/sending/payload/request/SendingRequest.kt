package com.kcvn.spm.app.transaction.sending.payload.request

import java.math.BigDecimal
import java.time.LocalDate

data class SendingRequest(
    var issueDate: LocalDate? = null,
    var locationCode: String? = null,
    var packageCode: String? = null,
    var poNumber: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO
)
