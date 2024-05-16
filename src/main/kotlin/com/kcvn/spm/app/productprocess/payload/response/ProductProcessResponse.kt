package com.kcvn.spm.app.productprocess.payload.response

import com.kcvn.spm.common.payload.DropdownResponse
import com.kcvn.spm.common.payload.model.CellStyleModel

data class ProductProcessResponse (
    var processId: String? = null,
    var productName: String? = null,
    var layerCode: String? = null,
    var processCode: String? = null,
    var processName: String? = null,
    var processNameJp: String? = null,
    var processConvertCode: String? = null,
    var processStatisticCode: String? = null,
    var processInventoryCode: String? = null,
    var idx: Int? = 0,
    var productId: String? = null,
    var processProcedureStructureId: String? = null,
    var layerCodeInt: Int? = null,
    var processSequence: Int? = null,
    var listDropDownConvertCode : List<DropdownResponse>? = null,
    var listDropDownStatisticCode: List<DropdownResponse>? = null,
    var inventoryLayerGroup: String? = null,
    var dayOfImplementation: String? = null,
    var cellStyles: List<CellStyleModel> = mutableListOf()
)

