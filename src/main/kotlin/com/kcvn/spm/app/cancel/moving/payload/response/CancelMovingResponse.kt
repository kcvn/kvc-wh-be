package com.kcvn.spm.app.cancel.moving.payload.response

data class CancelMovingResponse(
    var sourceLocationCode: String? = null,
    var destLocationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0,
    var seqNo : Int? = 1
)
