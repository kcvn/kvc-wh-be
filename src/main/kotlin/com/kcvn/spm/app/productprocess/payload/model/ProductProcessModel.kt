package com.kcvn.spm.app.productprocess.payload.model

data class ProductProcessModel (
    var productName: String? = null,
    var layerCode: String? = null,
    var processCode: String? = null,
    var processName: String? = null,
    var processNameJp: String? = null,
    var processGroup: String? = null,
    var processConvertCode: String? = null,
    var processStatisticCode: String? = null,
    var processInventoryCode: String? = null,
    var processProcedureStructureId: String? = null,
    var processSequence: Int? = null,
    var inventoryLayerGroup: String? = null,
    var dayOfImplementation: String? = null,
    var unit: String? = null
)