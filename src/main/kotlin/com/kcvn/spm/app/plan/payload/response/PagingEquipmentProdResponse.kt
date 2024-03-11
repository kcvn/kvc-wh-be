package com.kcvn.spm.app.plan.payload.response

import com.kcvn.spm.app.plan.payload.model.EquipmentProductivityModel
import com.kcvn.spm.app.order.payload.response.CalendarValueResponse
import com.kcvn.spm.common.payload.BasePagingResponse



data class PagingEquipmentProdResponse(
    var columns: List<CalendarValueResponse>? = null
) : BasePagingResponse<EquipmentProductivityModel>()