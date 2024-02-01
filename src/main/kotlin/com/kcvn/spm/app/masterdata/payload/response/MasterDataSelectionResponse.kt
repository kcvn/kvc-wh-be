package com.kcvn.spm.app.masterdata.payload.response

import com.kcvn.spm.common.payload.DropdownResponse

data class MasterDataSelectionResponse (
    val frame1Selections: List<DropdownResponse> = listOf(),
    val frame2Selections: List<DropdownResponse> = listOf(),
    val moldSelections: List<DropdownResponse> = listOf(),
    val exportTypeSelections: List<DropdownResponse> = listOf(),
    val srNosrSelections: List<DropdownResponse> = listOf(),
    val tapeTypeSelections: List<DropdownResponse> = listOf(),
    val processConvertCodes:  List<DropdownResponse> = listOf(),
    val processStatisticCodes: List<DropdownResponse> = listOf(),
)
