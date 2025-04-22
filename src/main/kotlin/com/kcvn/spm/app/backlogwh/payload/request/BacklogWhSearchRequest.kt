package com.kcvn.spm.app.backlogwh.payload.request

import java.time.OffsetDateTime

class BacklogWhSearchRequest {
    var listLocationCode: String? = null
    var listPoNumber: String? = null
    var listPackageCode: String? = null
    var fromDate: OffsetDateTime? = null
    var toDate: OffsetDateTime? = null
}