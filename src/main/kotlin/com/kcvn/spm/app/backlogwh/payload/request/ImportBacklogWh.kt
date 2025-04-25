package com.kcvn.spm.app.backlogwh.payload.request

import java.time.LocalDate

data class ImportBacklogWh(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var receivingDate: LocalDate? = null,
    var inspectionDate: LocalDate? = null
)
