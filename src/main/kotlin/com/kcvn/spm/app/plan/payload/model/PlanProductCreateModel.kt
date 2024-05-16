package com.kcvn.spm.app.plan.payload.model

data class PlanProductCreateModel(
    var productName: String? = null,
    var frame_1: String? = null,
    var mold: String? = null,
    var pcsSh: Int? = null,
    var blockSh: Int? = null,
    var planProcesses: MutableList<PlanProcessCreateModel> = mutableListOf()
)
