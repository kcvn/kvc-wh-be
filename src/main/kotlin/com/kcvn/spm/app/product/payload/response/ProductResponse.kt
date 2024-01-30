package com.kcvn.spm.app.product.payload.response

import com.kcvn.spm.common.payload.DropdownResponse

data class ProductResponse (
    var id: String? = null,
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
    var snapMold: String? = null,
    var tapeCommon: String? = null,
    var tapeType: String? = null,
    var completionRate: Double? = 0.0,
    var productLayerDetail: String? = null,
    var process : Int? = 0,
    var lstProcess: List<DropdownResponse> = listOf()
)