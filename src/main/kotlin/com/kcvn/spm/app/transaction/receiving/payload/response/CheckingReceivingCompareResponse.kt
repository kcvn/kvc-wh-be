package com.kcvn.spm.app.transaction.receiving.payload.response

import java.math.BigDecimal
import java.time.LocalDate

/**
 * 1 dong = 1 invoice cua (ngay nhan hang, PO, lot).
 * receivedQty la so da nhan duoc PHAN BO DAN cho tung invoice (theo thu tu invoice), chi de xem; thuc te receiving khong chia theo invoice.
 * Dong co invoiceNumber = null: so da nhan vuot qua tong so da check (hoac da nhan ma khong co du lieu check).
 */
data class CheckingReceivingCompareResponse(
    var receivingDate: LocalDate? = null,
    var poNumber: String? = null,
    var lotNo: String? = null,
    var invoiceNumber: String? = null,
    var itemCode: String? = null,
    var itemName: String? = null,
    var checkedQty: BigDecimal? = null,
    var receivedQty: BigDecimal? = null,
    /** checkedQty - receivedQty; > 0: chua nhan du, < 0: nhan vuot */
    var diffQty: BigDecimal? = null,
    /** MATCHED | NOT_RECEIVED | PARTIAL | OVER */
    var result: String? = null,
)
