package com.kcvn.spm.app.report.quantityreport.payload.response

import java.time.OffsetDateTime

data class InformationCalculateQuantityResponse (
    var monthReport: OffsetDateTime? = null,
    var productName: String? = null,
    var processStatistic: String? = null,
    var totalQuantityOfProcess: Int? = null,
    var calculateQuantityResultId: String? = null,
    var monthNumber: Int? = null,
    var yearNumber: Int? = null,
    var orderDateFromTo: String? = null
)