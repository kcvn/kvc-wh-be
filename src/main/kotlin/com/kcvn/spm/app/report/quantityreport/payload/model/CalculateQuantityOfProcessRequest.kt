package com.kcvn.spm.app.report.quantityreport.payload.model

import org.apache.poi.hpsf.Decimal
import java.time.OffsetDateTime

data class CalculateQuantityOfProcessRequest (
    val version:Int? = null,
    val productName: String? = null,
    val blockSh: Int? = null,
    val processCount: Int? = null,
    val orderDate: OffsetDateTime? = null,
    val quantity: Int? = null,
    val completionRate: Double? = 0.0,
    val effectiveDate: OffsetDateTime? = null,
    val expirationDate: OffsetDateTime? = null,
)