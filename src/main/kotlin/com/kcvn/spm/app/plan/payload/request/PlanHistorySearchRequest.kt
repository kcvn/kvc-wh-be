package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime


data class PlanHistorySearchRequest(
    var productName: String? = null,
    var processGroups: String? = null,
    var frame_1: String? = null,
    var mold: String? = null,
    var planId: String? = null
)
