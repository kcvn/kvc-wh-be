package com.kcvn.spm.app.checking.payload.request

data class CheckingRequestWithSeq(
    var poNumber: String? = null,
    var packageCode: String? = null,
    var qty : Int? = 0,
    var seqNo : Int? = 1
)
