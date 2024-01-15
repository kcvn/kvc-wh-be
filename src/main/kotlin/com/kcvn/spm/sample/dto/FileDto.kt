package com.kcvn.spm.sample.dto

data class FileDto (
    var fileName: String? = null,
    var contentType: String? = null,
    var content: ByteArray? = null
)