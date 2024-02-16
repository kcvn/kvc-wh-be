package com.kcvn.spm.app.productprocess.payload.response

data class ImportProcessResponse (
    var productName: String,
    var processCode: String,
    var layerCode: String,
    var processConvertCode: String,
    var processStatisticCode: String,
    var processInventoryCode: String? = null,
    var id: String
)