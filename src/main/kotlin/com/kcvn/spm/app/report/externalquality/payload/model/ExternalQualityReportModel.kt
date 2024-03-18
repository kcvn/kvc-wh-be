package com.kcvn.spm.app.report.externalquality.payload.model

import com.kcvn.spm.common.payload.KeyValueResponse
import java.math.BigDecimal

data class ExternalQualityReportModel(
    var productName: String? = null,
    var productShortcutName: String? = null,
    var mold: String? = null,
    var exportType: String? = null,
    var pcsSh: Int? = null,
    var blockSh: Int? = null,
    var productLine: String? = null,
    var snapMold: String? = null,
    var layerCount: Int? = null,
    var tapeCommon: String? = null,
    var completionRate: BigDecimal? = null,
    var sumOrderQuantity: Int? = null,
    var sumWorkResultQuantity: Int? = null,
    var sumNeedExportQuantity: Int? = null,
    var tapeInventoryQuantity: Int? = null,
    var tapeExpireQuantity: Int? = null,
    var shippingData: List<KeyValueResponse> = listOf(),
    var details: List<ExternalQualityDetailModel> = listOf()
)
