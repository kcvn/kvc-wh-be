package com.kcvn.spm.app.splitting.payload.request

import java.time.LocalDate

data class SplittingRequest(
    var receivingDate: LocalDate? = null,
    var packageCode: String? = null,
    var locationCode: String? = null
)
