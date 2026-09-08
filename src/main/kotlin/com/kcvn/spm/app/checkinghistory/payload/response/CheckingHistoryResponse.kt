package com.kcvn.spm.app.checkinghistory.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class CheckingHistoryResponse(
    var lotNo: String? = null,
    var poNumber: String? = null,
    var invoiceNo: String? = null,
    var orderDate: LocalDate? = null,
    var itemCd: String? = null,
    var itemName: String? = null,
    var department: String? = null,
    var storageLocation: String? = null,
    var unit: String? = null,
    var itemType: String? = null,
    var seqNo: Int? = null,
    var orderQty: BigDecimal? = null,
    var scanQty: BigDecimal? = null,
    var scannedDate: LocalDate? = null,
    var scannedBy: String? = null,
    var result: Int? = null,
    var status: Boolean? = null
)