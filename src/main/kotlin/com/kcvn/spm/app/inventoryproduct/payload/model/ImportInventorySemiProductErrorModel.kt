package com.kcvn.spm.app.inventoryproduct.payload.model

import com.kcvn.spm.common.payload.model.CellStyleModel

data class ImportInventorySemiProductErrorModel(
    var productName: String? = null,
    var tapeLotNo: String? = null,
    var setQuantity: Int? = null,
    var blockQuantity: Int? = null,
    var sumBlockQuantity: Int? = null,
    var ngBlockQuantity: Int? = null,
    var successBlockQuantity: Int? = null,
    var messageError: String? = null,
    var cellStyles: List<CellStyleModel> = mutableListOf()
)
