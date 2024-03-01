package com.kcvn.spm.app.report.quantityreport.payload.model

import java.time.OffsetDateTime

data class ErrorOrderDetail(
    var productId:String? = null,
    var productName: String? = null,
    var orderDate: OffsetDateTime? = null,
)