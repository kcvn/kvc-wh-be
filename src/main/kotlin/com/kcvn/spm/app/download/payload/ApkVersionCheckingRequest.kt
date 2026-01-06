package com.kcvn.spm.app.download.payload

import java.math.BigDecimal

data class ApkVersionCheckingRequest(
    val deviceName: String = "",
    var currentVersion: String = "",
)