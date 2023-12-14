package com.kcvn.spm.common.payload.response

data class RoleResponse(
    var id: String,
    var name: String,
    var description: String?,
    val permissions: Set<String> = setOf()
)