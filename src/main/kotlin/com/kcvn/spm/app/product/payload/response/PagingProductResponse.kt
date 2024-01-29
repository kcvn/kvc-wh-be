package com.kcvn.spm.app.product.payload.response

import com.kcvn.spm.common.payload.BasePagingResponse

class PagingProductResponse : BasePagingResponse<ProductResponse>() {
    var collumns: List<String>? = null
}