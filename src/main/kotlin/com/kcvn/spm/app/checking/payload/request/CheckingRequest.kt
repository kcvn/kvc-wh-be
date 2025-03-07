package com.kcvn.spm.app.checking.payload.request

data class CheckingRequest(
    var poNumber: String? = null,
    var packageCode: String? = null,
    var qty : Int? = 0
)
