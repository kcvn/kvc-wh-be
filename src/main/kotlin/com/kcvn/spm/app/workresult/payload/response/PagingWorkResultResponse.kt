package com.kcvn.spm.app.workresult.payload.response

import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.DropdownResponse

data class PagingWorkResultResponse(
    var collumns: List<DropdownResponse>? = null,
) : BasePagingResponse<WorkResultResponse>()