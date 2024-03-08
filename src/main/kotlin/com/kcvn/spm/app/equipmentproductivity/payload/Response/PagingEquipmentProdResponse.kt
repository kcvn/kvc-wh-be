package com.kcvn.spm.app.equipmentproductivity.payload.Response

import com.kcvn.spm.app.equipmentproductivity.payload.Model.EquipmentProductivityModel
import com.kcvn.spm.app.order.payload.response.CalendarValueResponse
import com.kcvn.spm.common.payload.BasePagingResponse



data class PagingEquipmentProdResponse(
    var columns: List<CalendarValueResponse>? = null
) : BasePagingResponse<EquipmentProductivityModel>()