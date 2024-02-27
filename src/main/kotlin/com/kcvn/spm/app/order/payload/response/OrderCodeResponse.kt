package com.kcvn.spm.app.order.payload.response

import com.kcvn.spm.common.payload.DropdownResponse

data class OrderCodeResponse(
    var key: String? = null,
    var label: String? = null,
    val listVersion: List<DropdownResponse>
)
