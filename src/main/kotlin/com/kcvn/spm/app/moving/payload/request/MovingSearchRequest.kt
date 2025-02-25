package com.kcvn.spm.app.moving.payload.request

import java.time.OffsetDateTime

class MovingSearchRequest {
    var sourceLocationCode: String? = null
    var destLocationCode: String? = null
    var poNumber: String? = null
    var fromDate: OffsetDateTime? = null
    var toDate: OffsetDateTime? = null
}