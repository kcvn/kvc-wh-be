package com.kcvn.spm.payload.response

data class UserResponse(
    var id: Long,
    var username: String,
    var isAdmin: Boolean,
    val permissions: List<String> = listOf()
)