package com.kcvn.spm.app.cancel.receiving.payload.response

data class CancelRecTransResponse(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0,
    var seqNo : Int? = 1
)
