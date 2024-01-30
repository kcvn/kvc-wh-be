package com.kcvn.spm.common.payload

open class BasePagingResponse<T> (
    var data: List<T>? = null,
    var totalRecords: Int = 0
)