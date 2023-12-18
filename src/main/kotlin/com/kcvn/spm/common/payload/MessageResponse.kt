package com.kcvn.spm.common.payload

data class MessageResponse(
    var message: String,
    var data: Any? = null
)
