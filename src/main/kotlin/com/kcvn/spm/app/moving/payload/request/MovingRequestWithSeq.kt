package com.kcvn.spm.app.moving.payload.request

data class MovingRequestWithSeq(
    var sourceLocationCode: String? = null,
    var destLocationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0,
    var seqNo : Int? = 1
)
