package com.kcvn.spm.app.report.quantityreport.payload.model

import com.kcvn.spm.common.payload.KeyValueResponse
import java.time.OffsetDateTime

data class QuantityReportModel (
    var productName : String? = null,
    var monthNumber : Int? = null,
    var yearNumber : Int? = null,
    var orderDateFromTo: String? = null,
    var lstProcess: MutableList<KeyValueResponse> = mutableListOf()
)