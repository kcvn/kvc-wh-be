package com.kcvn.spm.app.inventoryproduct.payload.request

import java.time.OffsetDateTime

class InventorySemiProductSearchRequest {
    var productName : String? = null
    var tapeLotNo : String? = null
    var startDate : OffsetDateTime? = null
    var endDate : OffsetDateTime? = null
}