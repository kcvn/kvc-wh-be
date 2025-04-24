package com.kcvn.spm.app.stocktaking.payload.request

data class StopActualRequest(
    var yearNumber: Int? = null,
    var monthNumber: Int? = null
)
