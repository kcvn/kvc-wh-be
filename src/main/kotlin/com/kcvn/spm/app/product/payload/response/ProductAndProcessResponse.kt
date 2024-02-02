package com.kcvn.spm.app.product.payload.response

import com.kcvn.spm.app.productprocess.payload.response.ProductProcessResponse
import com.kcvn.spm.model.tables.pojos.Product
import com.kcvn.spm.model.tables.pojos.ProductProcess

data class ProductAndProcessResponse (
    var detail: Product? = null,
    var listProcess: List<ProductProcessResponse?>? = null
)
