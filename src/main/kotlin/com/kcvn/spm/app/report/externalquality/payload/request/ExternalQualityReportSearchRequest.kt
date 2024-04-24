package com.kcvn.spm.app.report.externalquality.payload.request

import java.time.OffsetDateTime

data class ExternalQualityReportSearchRequest(
    var productName: String? = null,
    var tapeCommon: String? = null,
    var mold: String? = null,
    var startDate: OffsetDateTime? = null,
    var endDate: OffsetDateTime? = null,
    var inventoryClosingDate: OffsetDateTime? = null
)
