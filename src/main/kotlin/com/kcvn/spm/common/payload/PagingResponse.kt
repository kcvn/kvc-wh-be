package com.kcvn.spm.common.payload

data class PagingResponse<T> (
    var data: List<T>,
    var totalRecords: Int
)