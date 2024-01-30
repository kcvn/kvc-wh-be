package com.kcvn.spm.app.product.payload.response

import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.DropdownResponse

data class PagingProductResponse (
    var collumns: List<DropdownResponse>? = null
) : BasePagingResponse<ProductResponse>()