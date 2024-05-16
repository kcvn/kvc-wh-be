package com.kcvn.spm.app.plan.payload.model

import java.math.BigDecimal

data class ProductPlanDetailModel(
    var frame_1: String? = null,
    var mold: String? = null,
    var layerCode: String? = null,
    var processCode: String? = null,
    var processName: String? = null,
    var processNameJp: String? = null,
    var completionRate: BigDecimal? = null,
    var processConvertCode: String? = null,
    var processStatisticCode: String? = null,
    var processGroup: String? = null,
    var processSequence: Int? = null,
    var inventory: Int? = null,
    var sumInventory: Int? = null,
    var processChildren: List<ProcessChildrenModel>? = listOf(),
    var planData: List<PlanDataByProcessModel>? = listOf(),

    var unit: String?=null

    )
