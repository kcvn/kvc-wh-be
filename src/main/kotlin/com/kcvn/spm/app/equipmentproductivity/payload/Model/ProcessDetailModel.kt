package com.kcvn.spm.app.equipmentproductivity.payload.Model

data class ProcessDetailModel(
    var name: String,
    var totalProcess: Int,
    var processDetailList: List<ProcessDetailListModel>
)