package com.kcvn.spm.app.stocktaking.payload.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDate

data class SystemStockTakingResponse(
    @JsonFormat(pattern = "dd-MM-yyyy")
    var inspectionDate: LocalDate? = null,
    var poNumber: String? = null,
    var amoebaLocationCode: String? = null,
    var systemLocationCode: String? = null,
    var amoebaQty: BigDecimal? = null,
    var systemQty: BigDecimal? = null,
    var result: String? = null
)
