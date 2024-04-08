package com.kcvn.spm.app.report.quantityreport.payload.request

import java.time.OffsetDateTime

class CalculateQuantityRequest {
    var monthReport: Int? = null
    var yearReport: Int? = null
    var startDate: OffsetDateTime? = null
    var endDate: OffsetDateTime? = null
}