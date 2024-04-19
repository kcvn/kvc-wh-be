package com.kcvn.spm.app.plan.payload.model

import java.math.BigDecimal
import java.time.OffsetDateTime

data class AllocationRateByDateModel (
    var orderDate: OffsetDateTime,
    var quantity: Int,
    var rate: BigDecimal
)