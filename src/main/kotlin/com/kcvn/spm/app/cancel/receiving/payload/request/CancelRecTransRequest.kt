package com.kcvn.spm.app.cancel.receiving.payload.request

data class CancelRecTransRequest(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0,
    var seqNo : Int? = 1
)
