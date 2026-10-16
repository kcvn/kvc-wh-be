package com.kcvn.spm.app.transaction.sending.payload.request

import java.math.BigDecimal

class TempSendingCheckingTransactionRequest {
    var clientScanId: String? = null
    var sendingPickingListId: String? = null
    var checkedQty: BigDecimal? = null
    var notMinusBoxQty: Boolean? = null
    var poNumber: String? = null
    var lotNo: String? = null
}
