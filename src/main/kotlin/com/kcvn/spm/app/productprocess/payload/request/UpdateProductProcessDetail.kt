package com.kcvn.spm.app.productprocess.payload.request

data class UpdateProductProcessDetailRequest(
    var listProcess: List<ItemUpdateProductProcessDetailRequest>? = null
)

data class ItemUpdateProductProcessDetailRequest(
    var processId: String? = null,
    var productName: String? = null,
    var layerCode: String? = null,
    var processCode: String? = null,
    var processName: String? = null,
    var processNameJp: String? = null,
    var processConvertCode: String,
    var processStatisticCode: String,
    var processInventoryCode: String? = null,
    var idx: Int,
    var inventoryLayerGroup: String? = null,
    var dayOfImplementation: Int? = null,
    var isEdit: Boolean? = false,
    var processSequence: String? = null
)