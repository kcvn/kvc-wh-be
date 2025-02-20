package com.kcvn.spm.app.transaction.receiving.payload.request

data class RecTransRequestWithSeq(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0,
    var seqNo : Int? = 1
)
