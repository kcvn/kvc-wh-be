package com.kcvn.spm.common.payload

data class PaginatedResponse (
    var data: List<Any>,
    var totalRecords: Int
)
