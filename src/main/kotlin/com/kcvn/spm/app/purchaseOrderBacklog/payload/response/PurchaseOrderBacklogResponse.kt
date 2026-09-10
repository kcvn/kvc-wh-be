package com.kcvn.spm.app.purchaseOrderBacklog.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class PurchaseOrderBacklogResponse(
    var seqNo: Int,
    var orderDate: LocalDate,
    var itemCode: String,
    var itemName: String,
    var prodGroup: String,
    var storageLocation: String,
    var orderQty: BigDecimal,
    var unit: String,
    var poNumber: String,
    var invoiceNumber: String,
    var detail: String,
    var itemType: String,
    var lotNo: String,
)
