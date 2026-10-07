package com.kcvn.spm.app.backlogwh.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class BacklogWhForSendingResponse(
    var poNumber: String? = null,
    var itemCode: String? = null,
    var productionGroup: String? = null,
    var itemName: String? = null,
    var receivingDate: LocalDate? = null,
    var lotNo: String? = null,
    var backlogQty: BigDecimal? = null,
    var availableBacklogQty: BigDecimal? = null,
)
