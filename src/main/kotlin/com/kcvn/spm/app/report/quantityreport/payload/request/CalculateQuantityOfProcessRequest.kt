package com.kcvn.spm.app.report.quantityreport.payload.request

import java.math.BigDecimal
import java.time.OffsetDateTime

data class CalculateQuantityOfProcessRequest(
    val version: Int? = null,
    val productName: String? = null,
    val blockSh: Int? = null,
    val processStatisticCode: String? = null,
    val processCount: Int? = null,
    val orderDate: OffsetDateTime? = null,
    val quantityBlock: Int? = null,
    val completionRate: BigDecimal? = null,
    val effectiveDate: OffsetDateTime? = null,
    val expirationDate: OffsetDateTime? = null,
    val quantityOfProcessStatistic : Int? = null,
)