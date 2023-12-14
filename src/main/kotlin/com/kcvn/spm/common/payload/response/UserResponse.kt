package com.kcvn.spm.common.payload.response

import java.time.LocalDate

data class UserResponse(
    var id: String,
    var username: String,
    var email: String? = null,
    var phoneNumber: String? = null,
    var fullName: String? = null,
    var dateOfBirth: LocalDate? = null,
    var avatar: String? = null,
    var isAdmin: Boolean,
    val roleIds: List<String> = listOf(),
    val permissionCodes: List<String> = listOf()
)