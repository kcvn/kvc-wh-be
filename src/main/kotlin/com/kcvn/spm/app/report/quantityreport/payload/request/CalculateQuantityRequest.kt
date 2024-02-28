package com.kcvn.spm.app.report.quantityreport.payload.request

import java.time.OffsetDateTime

class CalculateQuantityRequest {
    var fromDate : OffsetDateTime? = null
    var toDate : OffsetDateTime? = null
}