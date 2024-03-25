package com.kcvn.spm.app.order.payload.model

import com.kcvn.spm.common.payload.KeyValueResponse

data class OrderDetailModel (
    var productShortcutName: String? = null,
    var productName: String? = null,
    var quantity: Int? = null,
    var frame_1: String? = null,
    var layerCount: Int? = null,
    var pcsSh: Int? = null,
    var shBlock: Int? = null,
    var srNosr: String? = null,
    var version: String? = null,
    var quantityByCalendars : List<KeyValueResponse>? = listOf()

)