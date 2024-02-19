package com.kcvn.spm.app.workresult.payload.response

import java.time.OffsetDateTime

data class WorkResultResponse (
    val id : String? = null,
    val summaryResultDate : OffsetDateTime? = null,
    val itemName : String? = null,
    val processName : String? = null,
    val processCode : String? = null,
    val layerCode : String? = null,
    val totalTapeQuantity : Int? = null,
    val totalSheetQuantity : Int? = null,
    val goodTapeQuantity : Int? = null,
    val goodSheetQuantity : Int? = null,
    val performance : String? = null,
    val orderCode : String? = null,
    val tapeLotNo : String? = null,
    val code : String? = null,
    val workImplementBy : String? = null,
    val equipmentName : String? = null,
)