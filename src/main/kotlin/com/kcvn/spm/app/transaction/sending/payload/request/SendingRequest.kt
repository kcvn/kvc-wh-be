package com.kcvn.spm.app.transaction.sending.payload.request

import java.math.BigDecimal
import java.time.LocalDate

data class SendingRequest(
    var poNumber: String,
    var itemCode: String,
    var itemName: String? = null,
    var productionGroup: String? = null,
    var receivingDate: LocalDate,
    var lotNo: String,
    var requestQty: BigDecimal,
)
