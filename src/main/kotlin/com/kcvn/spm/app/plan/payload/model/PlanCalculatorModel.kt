package com.kcvn.spm.app.plan.payload.model

import com.kcvn.spm.app.productprocess.payload.model.ProductProcessModel
import com.kcvn.spm.model.tables.pojos.CompletionRateProcessProduct

data class PlanCalculatorModel(
    var processSource: ProductProcessModel? = null,
    var completionRateSource: CompletionRateProcessProduct? = null,
    var planDetailSource: MutableList<PlanDetailCreateModel> = mutableListOf(),
    var planProcessResults: List<PlanProcessCreateModel> = listOf()
)
