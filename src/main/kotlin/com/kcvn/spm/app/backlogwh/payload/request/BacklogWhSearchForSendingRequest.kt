package com.kcvn.spm.app.backlogwh.payload.request

import java.time.OffsetDateTime

class BacklogWhSearchForSendingRequest {
    var poNumber: String? = null
    var itemCode: String? = null
    var itemName: String? = null
    var productionGroup: String? = null
    var lotNo: String? = null
    var fromDate: OffsetDateTime? = null
    var toDate: OffsetDateTime? = null
}