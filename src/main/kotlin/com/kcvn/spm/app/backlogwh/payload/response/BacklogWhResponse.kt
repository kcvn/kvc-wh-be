package com.kcvn.spm.app.backlogwh.payload.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDate

data class BacklogWhResponse(
    var locationCode: String? = null,
    var poNumber: String? = null,
    var packageCode: String? = null,
    var backlogQty: BigDecimal? = null,
    var boxQty: Int? = null,
    @JsonFormat(pattern = "dd-MM-yyyy")
    var receivingDate: LocalDate? = null,
    @JsonFormat(pattern = "dd-MM-yyyy")
    var inspectionDate: LocalDate? = null
)
