package com.kcvn.spm.app.product.payload.response

import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.model.tables.pojos.ProductProcess

data class ProductAndProcessResponse (
    var listProduct: List<ProductResponse?>? = null,
    var listProcess: List<ProductProcessResponse?>? = null
)
