package com.kcvn.spm.app.transaction.sending.payload.request

import java.math.BigDecimal

class TempSendingTransactionRequest {
    var clientScanId: String? = null
    var sendingPickingListId: String? = null
    var scannedQty: BigDecimal? = null
    var notMinusBoxQty: Boolean? = null
    var packageCode: String? = null
    var lotNo: String? = null
}
