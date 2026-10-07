package com.kcvn.spm.app.backlogwh.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class BacklogWhResponse(
    var locationCode: String? = null,
    var itemCode: String? = null,
    var productionGroup: String? = null,
    var poNumber: String? = null,
    var packageCode: String? = null,
    var backlogQty: BigDecimal? = null,
    var boxQty: Int? = null,
    var receivingDate: LocalDate? = null,
    var itemName: String? = null,
    var lotNo: String? = null,
    var issueDate: String? = null,
    var availableBacklogQty: BigDecimal? = null,
)
