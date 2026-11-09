package com.kcvn.spm.app.splitting.payload.request

import java.time.LocalDate

class TempBacklogWhSearchRequest {
    var listLocationCode: String? = null
    var poNumber: String? = null
    var packageCode: String? = null
    var lotNo: String? = null
    var fromDate: LocalDate? = null
    var toDate: LocalDate? = null
    var isApproved: Boolean? = null
    /** null = lay tat ca; false = chia hang; true = chuyen lot */
    var isTransferLot: Boolean? = null
}
