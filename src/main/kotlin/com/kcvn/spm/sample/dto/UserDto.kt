package com.kcvn.spm.sample.dto

data class UserDto (
    var id: String? = null,
    var username: String? = null,
    var password: String? = null,
    var employeeCode: String? = null,
    var email: String? = null,
    var phoneNumber: String? = null,
    var fullName: String? = null,
    var fullNameUnsigned: String? = null,
    var dateOfBirth: String? = null,
    var status: Int? = null,
    var message: String? = null
)