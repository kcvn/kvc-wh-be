package com.kcvn.spm.app.stocktaking.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class SystemStockTakingResponse(
    var inspectionDate: LocalDate? = null,
    var poNumber: String? = null,
    var itemName: String? = null,
    var amoebaLocationCode: String? = null,
    var systemLocationCode: String? = null,
    var amoebaQty: BigDecimal? = null,
    var systemQty: BigDecimal? = null,
    var result: String? = null,
    var lotNo: String? = null,
    var issueDate: String? = null,
)
