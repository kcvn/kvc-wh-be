package com.kcvn.spm.app.purchaseOrderBacklog.payload.response

import java.math.BigDecimal

data class PurchaseOrderBacklogResponse(
    var poNumber: String? = null,
    var qty: BigDecimal? = null,
    var formCode: String? = null
)
