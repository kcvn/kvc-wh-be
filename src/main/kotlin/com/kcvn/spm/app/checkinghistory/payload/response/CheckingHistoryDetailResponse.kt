package com.kcvn.spm.app.checkinghistory.payload.response

import com.kcvn.spm.model.tables.pojos.ReceivingChecking
import java.math.BigDecimal
import java.time.LocalDate

data class CheckingHistoryDetailResponse(
    var lotNo: String? = null,
    var scanQty: BigDecimal? = null,
)