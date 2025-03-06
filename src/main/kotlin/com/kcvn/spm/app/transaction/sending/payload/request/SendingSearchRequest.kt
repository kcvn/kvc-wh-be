package com.kcvn.spm.app.transaction.sending.payload.request

import java.time.OffsetDateTime

class SendingSearchRequest {
    var listSourceLocationCode: String? = null
    var listDestLocationCode: String? = null
    var poNumber: String? = null
    var fromDate: OffsetDateTime? = null
    var toDate: OffsetDateTime? = null
}