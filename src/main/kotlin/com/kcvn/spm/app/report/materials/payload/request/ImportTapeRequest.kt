package com.kcvn.spm.app.report.materials.payload.request

import java.time.OffsetDateTime

class ImportTapeRequest {
    val monthReport: String? = null
    val yearReport: String? = null
    val startDate: OffsetDateTime? = null
    val endDate: OffsetDateTime? = null
    val hasUpdateTape: Boolean = false
}