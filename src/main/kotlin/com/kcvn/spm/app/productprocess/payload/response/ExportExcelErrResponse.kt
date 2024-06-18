package com.kcvn.spm.app.productprocess.payload.response

import com.kcvn.spm.common.payload.model.CellStyleModel

data class ExportExcelErrResponse (
    var productName: String? = null,
    var processCode: String? = null,
    var layerCode: String? = null,
    var processConvertCode: String? = null,
    var processStatisticCode: String? = null,
    var processInventoryCode: String? = null,
    var inventoryLayerGroup: String? = null,
    var dayOfImplementation: String? = null,
    var idProcessStructure: String? = null,
    var messageErrs: MutableList<String?>? = null,
    var cellStyles: List<CellStyleModel> = mutableListOf(),
    var processName: String? = null,
)