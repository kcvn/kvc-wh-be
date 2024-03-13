package com.kcvn.spm.app.plan.payload.model

import com.kcvn.spm.common.payload.KeyValueResponse

data class ProcessDetailListModel(
    var type: String,
    var quantityByCalendars: List<KeyValueResponse>
)