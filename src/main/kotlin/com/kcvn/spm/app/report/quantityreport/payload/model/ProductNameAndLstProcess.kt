package com.kcvn.spm.app.report.quantityreport.payload.model

import com.kcvn.spm.common.payload.KeyValueResponse

data class ProductNameAndLstProcess(
    var productName: String? = null,
    var lstProcess: List<KeyValueResponse> = listOf()
)
