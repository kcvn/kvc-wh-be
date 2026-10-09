package com.kcvn.spm.app.transaction.sending.payload.request

import java.time.LocalDate

// Khoá xác định 1 sending request: receiving_date + po_number + seq_no
class SendingRequestKey (
    var poNumber: String,
    var receivingDate: LocalDate,
    var seqNo: Int,
)
