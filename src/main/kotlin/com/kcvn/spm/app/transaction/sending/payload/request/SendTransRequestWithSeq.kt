package com.kcvn.spm.app.transaction.sending.payload.request

data class SendTransRequestWithSeq(
    var sourceLocationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0,
    var seqNo : Int? = 1
)
