package com.kcvn.spm.app.plan.payload.model

import com.kcvn.spm.model.tables.pojos.PlanDetailTemp
import com.kcvn.spm.model.tables.pojos.PlanProcessTemp
import com.kcvn.spm.model.tables.pojos.PlanProductTemp
import com.kcvn.spm.model.tables.pojos.PlanTemp

data class PlanTempModel(
    var plan: PlanTemp? = null,
    var planProducts: List<PlanProductTemp> = listOf(),
    var planProcesses: List<PlanProcessTemp> = listOf(),
    var planDetails: List<PlanDetailTemp> = listOf()
)
