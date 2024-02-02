package com.kcvn.spm.app.workresult.payload.request

import java.time.LocalDateTime

class WorkResultSearchRequest {
    val order : String ? = null
    val productName : String? = null
    val listProcessGroup : List<String>? = null
    val listProcessName : List<String>? = null
    val tapeLot : String? = null
    val code : String? = null
    val fromDate : LocalDateTime? = null
    val toDate : LocalDateTime? = null
}