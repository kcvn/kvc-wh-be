package com.kcvn.spm.app.checkinghistory.payload.request

import java.time.LocalDate
import java.time.OffsetDateTime

class CheckingHistorySearchRequest {
    var receivingDate: LocalDate? = null
    var poNumber: String? = null
    var invoiceNumber: String? = null
    var fromDate: OffsetDateTime? = null
    var toDate: OffsetDateTime? = null
    var status: String? = null
    var storageLocation: String? = null
    var itemType: String? = null
}