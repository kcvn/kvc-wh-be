package com.kcvn.spm.app.sync.payload.model

import java.time.OffsetDateTime

data class SyncWorkResultGroupModel (
    var KC_HINMEI: String? = null,
    var KOTEI_CD: String? = null,
    var SO_NO: String? = null,
    var KANRI_NO: String? = null,
    var JISSEKI_KEIJO_DATE: OffsetDateTime? = null
)