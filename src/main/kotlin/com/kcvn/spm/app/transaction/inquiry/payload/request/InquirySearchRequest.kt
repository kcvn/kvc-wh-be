package com.kcvn.spm.app.transaction.inquiry.payload.request

import java.time.OffsetDateTime

class InquirySearchRequest {
    var sourceLocationCode: String? = null
    var destLocationCode: String? = null
    var sourcePackageCode: String? = null
    var destPackageCode: String? = null
    var poNumber: String? = null
    var fromDate: OffsetDateTime? = null
    var toDate: OffsetDateTime? = null
}