package com.kcvn.spm.app.receivingtransactions.payload

data class RecTransRequest(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0
)
