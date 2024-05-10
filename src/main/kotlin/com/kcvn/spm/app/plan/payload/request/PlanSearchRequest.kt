package com.kcvn.spm.app.plan.payload.request

import java.time.OffsetDateTime

class PlanSearchRequest {
    var productName: String? = null
    var processGroups: String? = null
    var frame_1: String? = null
    var mold: String? = null
    var startDate: OffsetDateTime? = null
    var endDate: OffsetDateTime? = null
    var inventoryWorkPlan: Boolean? = null
    var draftWorkPlan: Boolean? = null
    var fileName: String? = null
}
