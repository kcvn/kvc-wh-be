package com.kcvn.spm.app.splitting.payload.respone

import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

data class TempBacklogWhResponse(
    var id: String? = null,
    var tempSplittingId: String? = null,
    var locationCode: String? = null,
    var itemCode: String? = null,
    var productionGroup: String? = null,
    var poNumber: String? = null,
    var packageCode: String? = null,
    var backlogQty: BigDecimal? = null,
    var boxQty: Int? = null,
    var receivingDate: LocalDate? = null,
    var itemName: String? = null,
    var lotNo: String? = null,
    var issueDate: String? = null,
    var isApproved: Boolean? = null,
    var isTransferLot: Boolean? = null,
    var createdBy: String? = null,
    var createdDate: OffsetDateTime? = null,
)
