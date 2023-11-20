package com.kcvn.spm.payload.response

data class RoleResponse(
    var id: Int,
    var name: String,
    var description: String?,
    val permissions: List<String> = listOf()
)