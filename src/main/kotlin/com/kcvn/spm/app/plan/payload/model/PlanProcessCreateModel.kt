package com.kcvn.spm.app.plan.payload.model

import java.math.BigDecimal

data class PlanProcessCreateModel(
    var processCode: String? = null,
    var processName: String? = null,
    var processConvertCode: String? = null,
    var layerCode: String? = null,
    var completionRate: BigDecimal? = null,
    var inventory: Int? = null,
    var unit: String? = null,
    var processSequence: Int? = null,
    var processNameJp: String? = null,
    var processGroup: String? = null,
    var processStatisticCode: String? = null,
    var childrenProcesses: MutableList<PlanChildrenProcessCreateModel> = mutableListOf(),
    var planDetails: MutableList<PlanDetailCreateModel> = mutableListOf()
)
