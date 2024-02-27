package com.kcvn.spm.app.product.payload.response

data class ProductDetailResponse(
    var name: String? = null,
    var exportType: String? = null,
    var size: String? = null,
    var frame_1: String? = null,
    var frame_2: String? = null,
    var mold: String? = null,
    var productLine: String? = null,
    var srNosr: String? = null,
    var pcsSh: Int? = null,
    var shBlock: Int? = null,
    var layerCount: Int? = null,
    var ringJig: String? = null,
    var process: Int? = null,
    var snapMold: String? = null,
    var tapeCommon: String? = null,
    var tapeType: String? = null,
    var productLayerDetail: String? = null,
    var rate: Double? = 0.0
)