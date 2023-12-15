package com.kcvn.spm.common.payload.response

data class PaginatedResponse (
    var data: List<Any>,
    var totalRecords: Int
)
