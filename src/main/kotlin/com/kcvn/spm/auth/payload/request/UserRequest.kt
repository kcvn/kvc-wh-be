package com.kcvn.spm.auth.payload.request

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

class UserRequest {
    var username: @NotBlank String? = null
    var password: @NotBlank String? = null
    var employeeCode: String? = null
    var email: String? = null
    var phoneNumber: String? = null
    var fullName: String? = null
    var dateOfBirth: LocalDate? = null
    var avatar: String? = null
    var status: Short? = null
    var roleIds: Set<String>? = null
    var positions: Set<String>? = null
}