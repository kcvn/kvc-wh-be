package com.kcvn.spm.app.cancel.sending.payload.request

data class CancelSendTransRequest(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0,
    var seqNo : Int? = 1
)
