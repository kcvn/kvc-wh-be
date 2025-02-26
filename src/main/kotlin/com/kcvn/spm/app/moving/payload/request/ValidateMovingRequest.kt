package com.kcvn.spm.app.moving.payload.request

data class ValidateMovingRequest(
    var sourceLocationCode: String? = null,
    var poNumber: String? = null,
    var qty : Int? = 0
)
