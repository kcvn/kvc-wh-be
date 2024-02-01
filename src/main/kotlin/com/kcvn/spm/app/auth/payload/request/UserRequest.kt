package com.kcvn.spm.app.auth.payload.request

import jakarta.validation.constraints.NotBlank
import java.time.LocalDate

class UserRequest {
    @field:NotBlank(message = "username must not be blank")
    var username: @NotBlank String? = null
    @field:NotBlank(message = "password must not be blank")
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