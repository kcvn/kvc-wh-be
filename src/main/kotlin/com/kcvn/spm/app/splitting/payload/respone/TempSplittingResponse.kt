package com.kcvn.spm.app.splitting.payload.respone

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

data class TempSplittingResponse(
    var id: String? = null,
    var locationCode: String? = null,
    var packageCode: String? = null,
    var receivingDate: LocalDate? = null,
    var poNumber: String? = null,
    var totalQty: BigDecimal? = null,
    var isApproved: Boolean? = null,
    var createdBy: String? = null,
    var createdDate: OffsetDateTime? = null,
)
