package com.kcvn.spm.app.backlog.payload.response

data class BacklogResponse(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var backlogQty: Int? = null,
    var boxQty: Int? = null
)
