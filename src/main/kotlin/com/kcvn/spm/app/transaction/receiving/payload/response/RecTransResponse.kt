package com.kcvn.spm.app.transaction.receiving.payload.response

import java.time.OffsetDateTime

data class RecTransResponse(
    var sourceLocationCode: String? = null,
    var destLocationCode: String? = null,
    var poNumber: String? = null,
    var qty: Int? = null,
    var seq: Int? = null,
    val createdDate: OffsetDateTime? = null
)
