package com.kcvn.spm.app.productprocess.payload.response

data class ProductProcessResponse (
    var id: String? = null,
    var productName: String? = null,
    var layerCode: String? = null,
    var processCode: String? = null,
    var processName: String? = null,
    var processNameJp: String? = null,
    var processConvertCode: String? = null,
    var processStatisticCode: String? = null,
    var processInventoryCode: String? = null,
    var productId: String? = null
)
