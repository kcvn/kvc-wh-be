package com.kcvn.spm.common.payload

import java.math.BigDecimal

data class KeyValueResponse (
    var key: String? = null,
    var value: String? = null,
    var sort: BigDecimal? = null,
    var isHasDifferent: Boolean? = null,
    var color: String? = null
)