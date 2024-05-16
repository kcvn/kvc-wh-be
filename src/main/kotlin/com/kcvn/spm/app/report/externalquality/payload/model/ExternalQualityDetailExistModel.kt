package com.kcvn.spm.app.report.externalquality.payload.model

import com.kcvn.spm.common.payload.KeyValueResponse
import java.math.BigDecimal

data class ExternalQualityDetailExistModel (
    var productName: String? = null,
    var mold: String? = null,
    var exportTypeConvert: String? = null,
    var pcsSh: Int? = null,
    var blockSh: Int? = null,
    var snapMold: String? = null,
    var completionRate: BigDecimal? = null,
    var quantityByCalendars: List<KeyValueResponse> = listOf()
)