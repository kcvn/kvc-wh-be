package com.kcvn.spm.app.inventoryproduct.payload.model

import com.kcvn.spm.common.payload.model.CellStyleModel


data class ImportInventoryErrorModel(
    var processCode: String? = null,
    var processName: String? = null,
    var code: String? = null,
    var layerCode: String? = null,
    var tapeLotNo: String? = null,
    var productName: String? = null,
    var orderCode: String? = null,
    var productQuantity: Int? = null,
    var sheetQuantity: Int? = null,
    var messageError: String? = null,
    var cellStyles: List<CellStyleModel> = listOf(),
    //new
    var employeeCode: String? = null,
    var team: String? = null,
    var processNameJp: String? = null,
    var processingDirective: Int? = null,
    var piecesPerSheet: Int? = null,
    var processCount: Int? = null,
    var seidenRepNumber: Int? = null,
    var productionAreaName: String? = null,

)
