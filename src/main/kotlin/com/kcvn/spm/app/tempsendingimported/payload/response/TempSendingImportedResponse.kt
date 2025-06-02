package com.kcvn.spm.app.tempsendingimported.payload.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.time.LocalDate

data class TempSendingImportedResponse(
    @JsonFormat(pattern = "dd-MM-yyyy")
    var inspectionDate: LocalDate? = null,
    var locationCode: String? = null,
    var poNumber: String? = null,
    var qty: String? = null
)
