package com.kcvn.spm.app.splitting.payload.respone

import java.math.BigDecimal
import java.time.OffsetDateTime

data class SplittingSapExportResponse(
    val itemCode: String?,
    val packageCode: String?,
    val lotNo: String?,
    val updatedDate: OffsetDateTime?,
    val productionGroup: String?,
    val storageLocation: String?,
    val quantity: BigDecimal?,
    val unit: String?,
)
