package com.kcvn.spm.app.product.payload.response

import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.model.tables.pojos.Product

data class ProductAndProcessResponse (
    var detail: ProductDetailResponse? = null,
    var listProcess: List<ProductProcessResponse?>? = null
)
