package com.kcvn.spm.app.transaction.receiving.payload.request

import java.time.OffsetDateTime

class RecTransSearchRequest {
    var locationCode: String? = null
    var poNumber: String? = null
    var fromDate: OffsetDateTime? = null
    var toDate: OffsetDateTime? = null
}