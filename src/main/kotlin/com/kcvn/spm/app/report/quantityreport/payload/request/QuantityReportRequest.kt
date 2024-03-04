package com.kcvn.spm.app.report.quantityreport.payload.request

import java.time.OffsetDateTime

class QuantityReportRequest {
    var productName: String? = null
    var startDate: OffsetDateTime? = null
    var endDate: OffsetDateTime? = null
}