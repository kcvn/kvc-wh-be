package com.kcvn.spm.app.product.payload.request

data class ProductImportRequest (
    var productName: String? = null,
    var exportType: String? = null,
    var size: String? = null,
    var frame_1: String? = null,
    var frame_2: String? = null,
    var mold: String? = null,
    var productLine: String? = null,
    var srNosr: String? = null,
    var pcs_sh: String? = null,
    var sh_block: String? = null,
    var layerCount: String? = null,
    var ring_jig: String? = null,
    var process: String? = null,
    var snapMold: String? = null,
    var tapeCommon: String? = null,
    var tapeType: String? = null
)