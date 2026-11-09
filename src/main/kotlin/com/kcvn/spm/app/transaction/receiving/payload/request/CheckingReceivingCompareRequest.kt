package com.kcvn.spm.app.transaction.receiving.payload.request

import java.time.LocalDate

class CheckingReceivingCompareRequest {
    /** ngay nhan hang tu ngay (yyyy-MM-dd) */
    var fromDate: LocalDate? = null
    /** ngay nhan hang den ngay (yyyy-MM-dd) */
    var toDate: LocalDate? = null
    /** 1 gia tri, tim gan dung (ilike) */
    var poNumber: String? = null
    /** 1 gia tri, tim gan dung (ilike) */
    var lotNo: String? = null
    /** 1 gia tri, tim gan dung (ilike); loc sau khi phan bo nen khong lam thay doi so phan bo */
    var invoiceNumber: String? = null
    /** SAME = chi lay dong khop, DIFFERENT = chi lay dong lech (checkedQty != receivedQty), null = khong loc */
    var result: String? = null
}
