package com.kcvn.spm.app.inventoryproduct.payload.response

import java.time.OffsetDateTime

data class InventoryProductResponse (
    var inventoryDate: OffsetDateTime?= null,
    var productQuantity: Int? = null,
    var sheetQuantity: Int? = null,
    var orderCode: String? = null,
    var tapeLotNo: String? = null,
    var productName: String? = null,
    var processName: String? = null,
    var processCode: String? = null,
    var layerCode: String? = null,
    var code: String? = null,
    var pcsSh: String? = null,
)