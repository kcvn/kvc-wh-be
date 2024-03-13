package com.kcvn.spm.app.productprocess.payload.response

data class ExportExcelErrResponse (
    var productName: String? = null,
    var processCode: String? = null,
    var layerCode: String? = null,
    var processConvertCode: String? = null,
    var processStatisticCode: String? = null,
    var processInventoryCode: String? = null,
    var inventoryLayerGroup: String? = null,
    var dayOfImplementation: Int? = null,
    var idProcessStructure: String? = null,
    var messageErrs: MutableList<String?>? = null
)