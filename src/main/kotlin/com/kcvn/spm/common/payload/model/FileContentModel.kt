package com.kcvn.spm.common.payload.model

data class FileContentModel (
    var fileName: String? = null,
    var contentType: String? = null,
    var content: ByteArray? = null
)