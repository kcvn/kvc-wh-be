package com.kcvn.spm.app.report.externalquality.payload.response

import com.kcvn.spm.app.report.externalquality.payload.model.ExternalQualityReportModel
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.CalendarResponse

data class ExternalQualityReportResponse(
    var columns: List<CalendarResponse> = listOf(),
    var subColumns: List<CalendarResponse> = listOf()
): BasePagingResponse<ExternalQualityReportModel>()
