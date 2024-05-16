package com.kcvn.spm.app.report.externalquality.payload.model

import com.kcvn.spm.common.payload.KeyValueResponse
import java.math.BigDecimal

data class ExternalQualityReportModel(
    var productName: String? = null,
    var productShortcutName: String? = null,
    var mold: String? = null,
    var exportType: String? = null,
    var exportTypeConvert: String? = null,
    var pcsSh: Int? = null,
    var blockSh: Int? = null,
    var productLine: String? = null,
    var snapMold: String? = null,
    var layerCount: Int? = null,
    var tapeCommon: String? = null,
    var completionRate: BigDecimal? = null,
    var sumOrderQuantity: Int? = null,
    var sumInventoryQuantity: Int? = null,
    var goodQualityTapeInventorySet: Int? = 0,
    var goodQualityTapeInventoryBlock: Int? =0,
    var tapeInventoryQuantity1: Int? = null,
    var tapeExpireQuantity1: Int? = null,
    var tapeInventoryQuantity2: Int? = null,
    var tapeExpireQuantity2: Int? = null,
    var exportType1: String? = null,
    var exportType2: String? = null,
    var exportTypes: List<KeyValueCustom> = listOf(),
    var shippingData: List<KeyValueResponse> = listOf(),
    var details: List<ExternalQualityDetailModel> = listOf()
)
