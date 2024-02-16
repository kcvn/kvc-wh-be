package com.kcvn.spm.app.auth.payload.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.LocalDate

data class UpdateUserRequest (
    @field:NotBlank(message = "user.username.notblank")
    @field:Size(min = 6,message = "user.username.size")
    var username: @NotBlank String? = null,
    var password: @NotBlank String? = null,
    var employeeCode: String? = null,
    @field:Email(message = "user.email")
    var email: String? = null,
    var phoneNumber: String? = null,
    var fullName: String? = null,
    var dateOfBirth: LocalDate? = null,
    var avatar: String? = null,
    var status: Short? = null,
    var roleIds: Set<String>? = null,
    var positions: Set<String>? = null,
)