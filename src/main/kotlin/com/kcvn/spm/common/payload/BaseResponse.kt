package com.kcvn.spm.common.payload

open class BaseResponse<T> (
    var data: T? = null,
    var message: String? = ""
)