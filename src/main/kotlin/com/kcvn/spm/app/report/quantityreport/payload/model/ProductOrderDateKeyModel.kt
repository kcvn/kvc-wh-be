package com.kcvn.spm.app.report.quantityreport.payload.model

import java.time.OffsetDateTime

data class ProductOrderDateKeyModel(
    val productName:String?,
    val monthNumber: Int?,
    val yearNumber: Int?,
    val orderDateFromTo: String?
)