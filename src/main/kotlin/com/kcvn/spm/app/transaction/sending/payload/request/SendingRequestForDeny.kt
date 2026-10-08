package com.kcvn.spm.app.transaction.sending.payload.request

import java.time.LocalDate
import java.time.OffsetDateTime

class SendingRequestForDeny (
    var poNumber: String,
    var receivingDate: LocalDate,
    var seqNo: Int,
    var comment: String,
)