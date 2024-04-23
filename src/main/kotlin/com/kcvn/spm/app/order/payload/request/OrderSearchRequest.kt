package com.kcvn.spm.app.order.payload.request

import com.google.gson.annotations.SerializedName
import java.time.OffsetDateTime

class OrderSearchRequest (
    var isChangeQuantity: Boolean = false,
    var productName: String? = null,
    var frame_1: String? = null,
    var srNosr: String? = null,
    var startDate: OffsetDateTime? = null,
    var endDate: OffsetDateTime? = null,
    var version: String? = null,

    )