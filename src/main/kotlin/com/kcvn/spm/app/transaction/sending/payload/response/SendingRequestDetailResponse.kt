package com.kcvn.spm.app.transaction.sending.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class SendingRequestDetailResponse(
    var poNumber: String? = null,
    var itemCode: String? = null,
    var itemName: String? = null,
    var productionGroup: String? = null,
    var receivingDate: LocalDate? = null,
    var seqNo: Int? = null,
    var lotNo: String? = null,
    var backlogQty: BigDecimal? = null,
    var availableBacklogQty: BigDecimal? = null,
    var requestQty: BigDecimal? = null,
    var status: Int? = null,
    var comment: String? = null,
)
