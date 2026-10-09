package com.kcvn.spm.app.transaction.sending.payload.request

import java.time.LocalDate

class SendingRequestForConfirm (
    var poNumber: String,
    var receivingDate: LocalDate,
    var seqNo: Int,
    var formCode: String,
)