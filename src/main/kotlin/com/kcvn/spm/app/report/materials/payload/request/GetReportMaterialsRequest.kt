package com.kcvn.spm.app.report.materials.payload.request

import java.time.OffsetDateTime

class GetReportMaterialsRequest {
    var startTime: OffsetDateTime? = null
    var endTime: OffsetDateTime? = null
    var productName: String? = null
    var tapeShared: String? = null
    var typeTape: String? = null
}