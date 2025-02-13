package com.kcvn.spm.app.backlog.payload.response

data class BacklogResponse(
    var locationCode: String,
    var poNumber: String,
    var backlogQty: Int,
    var boxQty: Int
)
