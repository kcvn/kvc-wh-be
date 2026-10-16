package com.kcvn.spm.app.transaction.sending.payload.response

import com.fasterxml.jackson.annotation.JsonFormat
import java.math.BigDecimal
import java.time.LocalDate
import java.time.OffsetDateTime

data class SendingRequestListResponse(
    var poNumber: String? = null,
    var itemCode: String? = null,
    var itemName: String? = null,
    var productionGroup: String? = null,
    var receivingDate: LocalDate? = null,
    var seqNo: Int? = null,
    var backlogQty: BigDecimal? = null,
    var availableQty: BigDecimal? = null,
    var requestQty: BigDecimal? = null,
    var status: Int? = null,
    var createdBy: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    var createdDate: OffsetDateTime? = null,
    var updatedBy: String? = null,
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "Asia/Ho_Chi_Minh")
    var updatedDate: OffsetDateTime? = null,
)
