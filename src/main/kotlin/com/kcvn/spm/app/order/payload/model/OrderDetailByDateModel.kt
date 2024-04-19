package com.kcvn.spm.app.order.payload.model

data class OrderDetailByDateModel(
    var productName: String? = null,
    var version: String? = null,
    var orderDate: String? = null,
    var quantity: Int? = null,
    var isHasDifferent: Boolean? = null
)
