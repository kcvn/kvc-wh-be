package com.kcvn.spm.app.plan.payload.model

import java.math.BigDecimal

data class ProductPlanDetailModel(
    var layerCode: String? = null,
    var processCode: String? = null,
    var processName: String? = null,
    var completionRate: BigDecimal? = null,
    var processConvertCode: String? = null,
    var inventory: Int? = null,
    var sumInventory: Int? = null,
    var processChildren: List<ProcessChildrenModel>? = listOf(),
    var planData: List<PlanDataByProcessModel>? = listOf()
)
