package com.kcvn.spm.app.report.quantityreport.payload.response

import com.kcvn.spm.app.report.quantityreport.payload.model.QuantityReportModel
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.KeyValueResponse

data class PagingQuantityReportResponse (
    var columns: List<KeyValueResponse>? = null
) : BasePagingResponse<QuantityReportModel>()