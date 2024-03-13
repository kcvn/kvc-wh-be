package com.kcvn.spm.app.productprocess.payload.response

import com.kcvn.spm.model.tables.pojos.ProductProcess

data class DataDefaultExcel (
    var listData: MutableList<ProductProcessResponse?>? = null
) {
    init {
        listData = mutableListOf()
    }
}

