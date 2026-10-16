package com.kcvn.spm.app.transaction.sending.payload.request

import java.time.OffsetDateTime

class SendingRequestListSearchRequest {
    var poNumber: String? = null
    var itemCode: String? = null
    var itemName: String? = null
    var productionGroup: String? = null
    var status: Int? = null
}