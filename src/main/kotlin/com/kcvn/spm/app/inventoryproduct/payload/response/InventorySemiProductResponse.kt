package com.kcvn.spm.app.inventoryproduct.payload.response

import java.math.BigDecimal
import java.time.OffsetDateTime

data class InventorySemiProductResponse(
    var inventoryDate: OffsetDateTime? = null,
    var productName: String? = null,
    var tapeLotNo: String? = null,
    var completionRate: BigDecimal? = null,
    var blockSh: Int? = null,
    var setQuantity: Int? = null,
    var blockQuantity: Int? = null,
    var sumBlockQuantity: Int? = null,
    var ngBlockQuantity: Int? = null,
    var successBlockQuantity: Int? = null
)