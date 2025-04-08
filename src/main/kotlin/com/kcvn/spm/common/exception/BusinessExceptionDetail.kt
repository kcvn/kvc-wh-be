package com.kcvn.spm.common.exception

data class BusinessExceptionDetail(
    override val message: String?,
    val data: Any,
) : RuntimeException()