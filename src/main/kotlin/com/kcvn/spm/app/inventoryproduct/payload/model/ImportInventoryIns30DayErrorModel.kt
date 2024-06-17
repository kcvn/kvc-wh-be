package com.kcvn.spm.app.inventoryproduct.payload.model

import com.kcvn.spm.common.payload.model.CellStyleModel
import java.time.OffsetDateTime

data class ImportInventoryIns30DayErrorModel(
    var inventoryDate: OffsetDateTime? = null,
    var productName: String? = null,
    var code: String? = null,
    var processCode: String? = null,
    var layerCode: String? = null,
    var productQuantity: Int? = null,
    var messageError: String? = null,
    var cellStyles: List<CellStyleModel> = mutableListOf()
)
