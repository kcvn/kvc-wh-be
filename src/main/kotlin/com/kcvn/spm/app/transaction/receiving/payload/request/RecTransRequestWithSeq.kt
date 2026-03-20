package com.kcvn.spm.app.transaction.receiving.payload.request

import java.math.BigDecimal

data class RecTransRequestWithSeq(
    var locationCode: String? = null,
    var packageCode: String? = null,
    var poNumber: String? = null,
    var qty : BigDecimal? = BigDecimal.ZERO,
    var seqNo : Int? = 1,
    var lotNo: String? = null,
    var issueDate: String? = null
)
