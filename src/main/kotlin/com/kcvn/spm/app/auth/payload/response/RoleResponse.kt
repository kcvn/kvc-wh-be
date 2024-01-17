package com.kcvn.spm.app.auth.payload.response

data class RoleResponse(
    var id: String,
    var name: String,
    var description: String?,
    val permissions: Set<String> = setOf()
)