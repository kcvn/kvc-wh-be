package com.kcvn.spm.app.splitting.payload.request

import java.math.BigDecimal
import java.time.LocalDate

data class SplittingRequest(
    var receivingDate: LocalDate,
    var binCode: String,
    var packageCode: String,
    var poNumber: String,
    var lotNo: String,
    var quantity: BigDecimal,
    var autoPackage: Boolean,
)
