package com.kcvn.spm.app.transaction.sending.payload.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.OffsetDateTime

data class SendingResponse(
    var sourceLocationCode: String? = null,
    var destLocationCode: String? = null,
    var poNumber: String? = null,
    var qty: BigDecimal? = BigDecimal.ZERO,
    var seq: Int? = null,
    @JsonFormat(pattern = "dd-MM-yyyy")
    val createdDate: OffsetDateTime? = null
)
