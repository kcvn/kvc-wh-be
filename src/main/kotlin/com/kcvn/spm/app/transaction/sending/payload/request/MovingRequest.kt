package com.kcvn.spm.app.transaction.sending.payload.request

data class MovingRequest(
    var sourceLocationCode: String? = null,
    var destLocationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0
)
