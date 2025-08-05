package com.kcvn.spm.app.transaction.sending.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class TempSendingInquiryResponse(
    var formCode: String? = null,
    var inspectionDate: LocalDate? = null,
    var locationCode: String? = null,
    var poNumber: String? = null,
    var requestQty: BigDecimal? = BigDecimal.ZERO,
    var actualQty: BigDecimal? = BigDecimal.ZERO,
    var doubleCheckQty: BigDecimal? = BigDecimal.ZERO,
    var isApproved: Boolean? = false,
    var result: String? = null,
    val createdDate: LocalDate? = null
)
