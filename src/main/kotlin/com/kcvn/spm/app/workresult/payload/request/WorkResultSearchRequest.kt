package com.kcvn.spm.app.workresult.payload.request

import java.time.OffsetDateTime

class WorkResultSearchRequest {
    var order : String ? = null
    var itemName : String? = null
    var listProcessGroup : Array<String>? = null
    var listProcessCode : Array<String>? = null
    var tapeLot : String? = null
    var code : String? = null
    var fromDate : OffsetDateTime? = null
    var toDate : OffsetDateTime? = null
}