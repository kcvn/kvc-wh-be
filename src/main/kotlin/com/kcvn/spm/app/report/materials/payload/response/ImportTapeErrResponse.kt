package com.kcvn.spm.app.report.materials.payload.response

data class ImportTapeErrResponse (
    val productName : String? = null,
    val tapeShared: String? = null,
    val typeTape: String? = null,
    val unitTape: Double? = null,
    val listMessageErr: MutableList<String?> = mutableListOf()
)