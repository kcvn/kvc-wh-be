package com.kcvn.spm.app.transaction.sending.payload.request

data class ValidateSendTransRequest(
    var sourceLocationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0
)
