package com.kcvn.spm.app.transaction.receiving.payload.request

data class RecTransRequest(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0
)
