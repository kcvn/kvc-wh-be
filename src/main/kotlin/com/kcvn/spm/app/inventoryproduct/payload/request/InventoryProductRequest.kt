package com.kcvn.spm.app.inventoryproduct.payload.request

import java.time.OffsetDateTime

class InventoryProductRequest {
    var orderCode : String ? = null
    var productName : String? = null
    var listProcessGroup : String? = null
    var listProcessCode : String? = null
    var tapeLot : String? = null
    var code : String? = null
    var fromDate : OffsetDateTime? = null
    var toDate : OffsetDateTime? = null
}