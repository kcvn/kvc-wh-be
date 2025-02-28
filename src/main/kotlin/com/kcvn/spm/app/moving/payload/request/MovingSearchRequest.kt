package com.kcvn.spm.app.moving.payload.request

import java.time.OffsetDateTime

class MovingSearchRequest {
    var listSourceLocationCode: String? = null
    var listDestLocationCode: String? = null
    var poNumber: String? = null
    var fromDate: OffsetDateTime? = null
    var toDate: OffsetDateTime? = null
}