package com.kcvn.spm.app.product.payload.response

import com.kcvn.spm.app.product.payload.model.ProductModel
import com.kcvn.spm.common.payload.BasePagingResponse
import com.kcvn.spm.common.payload.KeyValueResponse

data class PagingProductResponse (
    var columns: List<KeyValueResponse>? = null
) : BasePagingResponse<ProductModel>()