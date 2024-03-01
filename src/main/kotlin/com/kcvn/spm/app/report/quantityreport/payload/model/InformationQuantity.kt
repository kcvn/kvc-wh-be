package com.kcvn.spm.app.report.quantityreport.payload.model

import java.math.BigDecimal
import java.time.OffsetDateTime

data class InformationQuantity(
    var monthReport : OffsetDateTime? = null,
    var productName: String ? = null,
    var processStatisticCode :String? = null,
    var totalQuantityOfProcess: Int? = null,
    var createdDate:OffsetDateTime? = null,
    var createdBy:String? = null
)