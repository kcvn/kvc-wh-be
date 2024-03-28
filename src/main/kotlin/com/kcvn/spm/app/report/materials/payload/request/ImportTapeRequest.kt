package com.kcvn.spm.app.report.materials.payload.request

import java.time.OffsetDateTime

class ImportTapeRequest {
    var monthReport: String? = null
    var yearReport: String? = null
    var startDate: OffsetDateTime? = null
    var endDate: OffsetDateTime? = null
    var existTape: Boolean = false
}