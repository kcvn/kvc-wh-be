package com.kcvn.spm.app.plan.payload.response

import com.kcvn.spm.app.plan.payload.model.ProductPlanDetailModel
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.KeyValueResponse

data class ProductPlanDetailResponse(
    var columns: List<KeyValueResponse>? = null
) : BaseResponse<List<ProductPlanDetailModel>>()
