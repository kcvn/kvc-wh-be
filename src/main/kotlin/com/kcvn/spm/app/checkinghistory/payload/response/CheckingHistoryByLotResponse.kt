package com.kcvn.spm.app.checkinghistory.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class CheckingHistoryByLotResponse(
    var checkingHistoryId: String? = null,
    var receivingDate: LocalDate? = null,
    var poNumber: String? = null,
    var invoiceNumber: String? = null,
    var department: String? = null,
    var storageLocation: String? = null,
    var lotNo: String? = null,
    var itemType: String? = null,
    var seqNo: Int? = null,
    var scanQty: BigDecimal? = null,
    var scannedDate: LocalDate? = null,
    var scannedBy: String? = null,
    var status: Boolean? = null
)