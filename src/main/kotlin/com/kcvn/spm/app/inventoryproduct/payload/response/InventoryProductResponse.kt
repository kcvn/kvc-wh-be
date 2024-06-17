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
    var processNameJp: String? = null,
    var team: String? = null,
    var processingDirective: Int? = null,
    var piecesPerSheet : Int? = null,
    var productionAreaName: String? = null,
    var processCount: Int? = null,
    var seidenRepNumber:Int? = null,
    var employeeCode: String? = null,
    var successQuantity: Int? = null,
    var ins_30DayQuantity: Int? = null
)