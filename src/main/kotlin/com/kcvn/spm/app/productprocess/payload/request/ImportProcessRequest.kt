package com.kcvn.spm.app.productprocess.payload.request

data class ImportProcessRequest (
    var productName: String? = null,
    var layerCode: String? = null,
    var processCode: String? = null,
)