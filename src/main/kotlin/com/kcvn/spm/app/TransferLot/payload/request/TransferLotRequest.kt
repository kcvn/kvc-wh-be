package com.kcvn.spm.app.TransferLot.payload.request

import java.math.BigDecimal
import java.time.LocalDate

data class TransferLotRequest(
    var receivingDate: LocalDate,
    var binCode: String,
    var packageCode: String,
    var poNumber: String,
    var lotNo: String,
    var quantity: BigDecimal,
    var issueDate: LocalDate? = null,
)
