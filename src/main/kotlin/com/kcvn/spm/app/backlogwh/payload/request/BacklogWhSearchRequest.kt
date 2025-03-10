package com.kcvn.spm.app.backlogwh.payload.request

import java.time.LocalDate

class BacklogWhSearchRequest {
    var listLocationCode: String? = null
    var listPoNumber: String? = null
    var listPackageCode: String? = null
    var receivingDate: LocalDate? = null
    var issueDate: LocalDate? = null
}