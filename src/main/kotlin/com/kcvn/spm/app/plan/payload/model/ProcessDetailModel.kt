package com.kcvn.spm.app.plan.payload.model

data class ProcessDetailModel(
    var name: String? =null,
    var totalProcess: Int?,
    var processDetailList: List<ProcessDetailListModel> ?= listOf()
)