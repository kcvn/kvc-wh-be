package com.kcvn.spm.app.backlogwh.payload.request

import java.math.BigDecimal

data class BacklogWhUpdateRequest(
    var packageCode: String? = null,
    var backlogQty: BigDecimal? = null,
    var boxQty: Int? = null,
)
