package com.kcvn.spm.common.payload

data class FileResponse (
    var fileName: String? = null,
    var contentType: String? = null,
    var content: ByteArray? = null
)