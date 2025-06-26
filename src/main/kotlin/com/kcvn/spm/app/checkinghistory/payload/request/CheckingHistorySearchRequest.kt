package com.kcvn.spm.app.checkinghistory.payload.request

import java.time.OffsetDateTime

class CheckingHistorySearchRequest {
    var poNumber: String? = null
    var formCode: String? = null
    var fromDate: OffsetDateTime? = null
    var toDate: OffsetDateTime? = null
}