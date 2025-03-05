package com.kcvn.spm.app.cancel.sending.payload.response

data class CancelSendTransResponse(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0,
    var seqNo : Int? = 1
)
