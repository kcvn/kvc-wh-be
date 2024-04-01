package com.kcvn.spm.app.report.materials.payload.response

import com.kcvn.spm.common.payload.model.CellStyleModel
import java.math.BigDecimal

data class ImportTapeErrResponse (
    var productName : String? = null,
    var tapeShared: String? = null,
    var typeTape: String? = null,
    var unitPrice: Double? = null,
    var listMessageErr: MutableList<String?> = mutableListOf(),
    var completionRateProduct: BigDecimal? = BigDecimal(0),
    var quantity: Int? = null,
    var blockSh: Int = 0,
    var quantityTape: Int = 0,
    var intoMoney: Double? = 0.0,
    var cellStyles: List<CellStyleModel> = mutableListOf(),
    var exportTye: String? = null
)