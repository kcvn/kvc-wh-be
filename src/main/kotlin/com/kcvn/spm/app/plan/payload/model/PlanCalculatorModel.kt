package com.kcvn.spm.app.plan.payload.model

import com.kcvn.spm.app.productprocess.payload.model.ProductProcessModel
import com.kcvn.spm.model.tables.pojos.CompletionRateProcessProduct

data class PlanCalculatorModel(
    var processSource: ProductProcessModel,
    var completionRateSource: CompletionRateProcessProduct,
    var planDetailSource: MutableList<PlanDetailCreateModel>,
    var planProcessResults: List<PlanProcessCreateModel>
)
