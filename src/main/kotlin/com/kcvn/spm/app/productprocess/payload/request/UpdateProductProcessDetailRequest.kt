package com.kcvn.spm.app.productprocess.payload.request

data class UpdateProductProcessDetailRequest(
   var listProcess: List<ItemUpdateProductProcessDetailRequest>? = null
)

data class ItemUpdateProductProcessDetailRequest(
    var id: String,
    var processConvertCode: String,
    var processStatisticCode: String,
    var processInventoryCode: String? = null
)