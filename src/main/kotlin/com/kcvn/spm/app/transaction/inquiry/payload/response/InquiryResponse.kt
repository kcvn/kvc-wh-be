package com.kcvn.spm.app.transaction.inquiry.payload.response

import java.math.BigDecimal
import java.time.OffsetDateTime

data class InquiryResponse(
    var sourceLocationCode: String? = null,
    var destLocationCode: String? = null,
    var sourcePackageCode: String? = null,
    var destPackageCode: String? = null,
    var poNumber: String? = null,
    var qty: BigDecimal? = BigDecimal.ZERO,
    var seq: Int? = null,
    var transactionType: String? = null,
    val createdDate: OffsetDateTime? = null
)
