package com.kcvn.spm.app.transaction.sending.payload.request

import java.math.BigDecimal
import java.time.LocalDate

data class SendTransRequestWithSeq(
    var formCode: String? = null,
    var inspectionDate: LocalDate? = null,
    var sourceLocationCode: String? = null,
    var packageCode: String? = null,
    var poNumber: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO,
    var notMinusBoxQty : Boolean? = null,
    var seqNo : Int? = 1
)
