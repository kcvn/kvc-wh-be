package com.kcvn.spm.app.report.externalquality.payload.model

import com.kcvn.spm.common.payload.KeyValueResponse

data class ExternalQualityDetailModel(
    var titleKey: String? = null,
    var title: String? = null,
    var inventory: Int? = null,
    var quantityByCalendars: List<KeyValueResponse> = listOf()
)
