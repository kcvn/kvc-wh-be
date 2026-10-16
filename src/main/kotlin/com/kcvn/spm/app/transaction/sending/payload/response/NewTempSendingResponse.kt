package com.kcvn.spm.app.transaction.sending.payload.response

import java.math.BigDecimal
import java.time.LocalDate

data class NewTempSendingResponse(
    var sendingPickingListId: String? = null,
    var poNumber: String? = null,
    var receivingDate: LocalDate? = null,
    var lotNo: String? = null,
    var locationCode: String? = null,
    var packageCode: String? = null,
    var itemName: String? = null,
    var productionGroup: String? = null,
    var requestQty: BigDecimal? = null,
    var scannedQty: BigDecimal? = null,
    var checkedQty: BigDecimal? = null,
    var status: String? = null,
    var isApproved: Boolean? = null,
)
