package com.kcvn.spm.common.payload.model

import java.time.OffsetDateTime

data class FileContentModel (
    var fileName: String? = null,
    var contentType: String? = null,
    var content: ByteArray? = null,
    var time: OffsetDateTime? = null
)