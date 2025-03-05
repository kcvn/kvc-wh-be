package com.kcvn.spm.app.transaction.sending.payload.request

data class SendingRequest(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0
)
