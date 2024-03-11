package com.kcvn.spm.app.plan.payload.response

import com.kcvn.spm.app.plan.payload.model.ProductPlanDetailModel
import com.kcvn.spm.common.payload.BaseResponse
import com.kcvn.spm.common.payload.CalendarResponse

data class ProductPlanDetailResponse(
    var columns: List<CalendarResponse>? = null
) : BaseResponse<List<ProductPlanDetailModel>>()
