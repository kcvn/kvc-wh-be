package com.kcvn.spm.app.backlogwh.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class BacklogWhResponse(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var packageCode: String? = null,
    var backlogQty: BigDecimal? = null,
    var boxQty: Int? = null,
    var receivingDate: LocalDate? = null,
    var inspectionDate: LocalDate? = null,
    var itemName: String? = null
)
