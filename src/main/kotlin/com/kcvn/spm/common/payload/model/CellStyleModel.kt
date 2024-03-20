package com.kcvn.spm.common.payload.model

import org.apache.poi.ss.usermodel.CellStyle

data class CellStyleModel (
    var index: Int = 0,
    var cellStyle: CellStyle
)