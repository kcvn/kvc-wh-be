package com.kcvn.spm.app.report.materials.payload.request

import java.time.OffsetDateTime

class ImportTapeRequest {
    var monthReport: Int? = null
    var yearReport: Int? = null
    var startDate: OffsetDateTime? = null
    var endDate: OffsetDateTime? = null
}