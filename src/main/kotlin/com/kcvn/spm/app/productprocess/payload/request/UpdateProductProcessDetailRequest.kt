package com.kcvn.spm.app.productprocess.payload.request

data class UpdateProductProcessDetailRequest(
    var id: String,
    var processConvertCode: String,
    var processStatisticCode: String,
    var processInventoryCode: String? = null
)