package com.kcvn.spm.app.report.quantityreport.payload.model

import com.kcvn.spm.common.payload.KeyValueResponse
import java.time.OffsetDateTime

data class QuantityReportModel (
    var productName : String? = null,
    var monthReport : String? = null,
    var lstProcess: List<KeyValueResponse> = listOf()
)