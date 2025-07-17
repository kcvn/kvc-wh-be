package com.kcvn.spm.app.tempsendingimported.payload.response

import java.time.LocalDate

data class TempSendingImportedResponse(
    var formCode: String? = null,
    var inspectionDate: LocalDate? = null,
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty: String? = null
)
