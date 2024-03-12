package com.kcvn.spm.app.plan.payload.response

import com.kcvn.spm.app.plan.payload.model.EquipmentProductivityModel
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.CalendarResponse


data class PagingEquipmentProdResponse(
    var columns: List<CalendarResponse>? = null
) : BasePagingResponse<EquipmentProductivityModel>()