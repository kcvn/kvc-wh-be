package com.kcvn.spm.app.stocktaking.payload.request

data class StartActualRequest(
    var yearNumber: Int? = null,
    var monthNumber: Int? = null
)
