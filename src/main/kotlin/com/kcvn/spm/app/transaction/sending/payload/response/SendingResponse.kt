package com.kcvn.spm.app.transaction.sending.payload.response

import java.math.BigDecimal
import java.time.OffsetDateTime

data class SendingResponse(
    var sourceLocationCode: String? = null,
    var destLocationCode: String? = null,
    var poNumber: String? = null,
    var qty: BigDecimal? = BigDecimal.ZERO,
    var seq: Int? = null,
    val createdDate: OffsetDateTime? = null
)
