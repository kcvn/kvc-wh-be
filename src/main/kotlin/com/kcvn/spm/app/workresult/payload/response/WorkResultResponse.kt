package com.kcvn.spm.app.workresult.payload.response

import java.time.OffsetDateTime

data class WorkResultResponse(
    val id: String? = null,
    val actualResultDepartment: String? = null,
    val team: String? = null,
    val processCode: String? = null,
    val processName: String? = null,
    val processNameJp: String? = null, // cai nay khong biet lay tu dau
    val summaryResultDate: OffsetDateTime? = null,
    val workStartTime: OffsetDateTime? = null,
    val workEndTime: OffsetDateTime? = null,
    val orderCode: String? = null,
    val itemName: String? = null,
    val customerCode: String? = null,
    val remediationDirectiveNumber: String? = null,
    val tapeLotNo: String? = null,
//chiu 14
    val code: String? = null,
    val layerCode: String? = null,
    val total: Int? = null,
    val totalTapeQuantity: Int? = null,
    val totalSheetQuantity: Int? = null,
    val goodItemQuantity: Int? = null,
    val goodTapeQuantity: Int? = null,
    val goodSheetQuantity: Int? = null,
    val performance: String? = null,
//chiu 24
    val departmentCode: String? = null,
    val workImplementBy: String? = null,
    val equipmentName: String? = null,
    val description: String? = null,

)