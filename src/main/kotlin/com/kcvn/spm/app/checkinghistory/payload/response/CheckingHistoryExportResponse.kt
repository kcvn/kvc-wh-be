package com.kcvn.spm.app.checkinghistory.payload.response

import com.opencsv.bean.CsvBindByName
import java.math.BigDecimal
import java.time.LocalDate

data class CheckingHistoryExportResponse(
    var scanDate: LocalDate? = null,
    var formCode: String? = null,
    var poNumber: String? = null,
    var importQty: BigDecimal? = null,
    var scanQty: BigDecimal? = null,
    var seqNo: Int? = null,
    var result: String? = null
)