package com.kcvn.spm.app.transaction.sending.payload.response

import java.math.BigDecimal
import java.time.OffsetDateTime

data class SendingSapExportResponse(
    val itemCode: String?,
    val lotNo: String?,
    val updatedDate: OffsetDateTime?,
    val productionGroup: String?,
    val storageLocation: String?,
    val quantity: BigDecimal?,
    val unit: String?,
)
