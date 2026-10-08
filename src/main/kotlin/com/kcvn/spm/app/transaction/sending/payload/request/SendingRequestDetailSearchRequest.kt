package com.kcvn.spm.app.transaction.sending.payload.request

import java.time.LocalDate
import java.time.OffsetDateTime

data class SendingRequestDetailSearchRequest (
    var poNumber: String,
    var receivingDate: LocalDate,
    var seqNo: Int
)