package com.kcvn.spm.app.plan.payload.model

data class ProductPlanModel(
    var id: String? = null,
    var productName: String? = null,
    var frame_1: String? = null,
    var mold: String? = null,
    var pcsSh: Int? = null,
    var blockSh: Int? = null
)