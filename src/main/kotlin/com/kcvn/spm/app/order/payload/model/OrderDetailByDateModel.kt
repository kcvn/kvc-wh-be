package com.kcvn.spm.app.order.payload.model

data class OrderDetailByDateModel(
    var orderId: String? = null,
    var productId: String? = null,
    var orderDate: String? = null,
    var quantity: Int? = null
)
