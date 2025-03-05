package com.kcvn.spm.app.transaction.sending.payload.response

data class ValidateSendTransResponse(
    var sourceLocationCode: String? = null,
    var poNumber: String? = null
)
