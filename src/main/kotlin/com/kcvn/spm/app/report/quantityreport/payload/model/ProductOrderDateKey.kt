package com.kcvn.spm.app.report.quantityreport.payload.model

import java.time.OffsetDateTime

data class ProductOrderDateKey(
    val productName:String?,
    val orderDate:OffsetDateTime?
)