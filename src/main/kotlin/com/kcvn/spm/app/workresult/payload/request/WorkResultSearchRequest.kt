package com.kcvn.spm.app.workresult.payload.request

import java.time.LocalDateTime

class WorkResultSearchRequest {
    var order : String ? = null
    var itemName : String? = null
    var listProcessGroup : Array<String>? = null
    var listProcessName : Array<String>? = null
    var tapeLot : String? = null
    var code : String? = null
    var fromDate : LocalDateTime? = null
    var toDate : LocalDateTime? = null
}