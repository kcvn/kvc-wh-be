package com.kcvn.spm.app.auth.payload.request

import jakarta.validation.constraints.Email
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import java.time.LocalDate
import kotlin.math.min

class UserRequest {
    @field:NotBlank(message = "user.username.notblank")
    @field:Size(min = 6,message = "user.username.size")
    var username: @NotBlank String? = null
    @field:NotBlank(message = "user.password.notblank")
    @field:Size(min = 6, message = "user.password.size")
    @field:Pattern(
        regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=.])(?=\\S+\$).{6,}\$",
        message = "user.password.pattern"
    )
    var password: @NotBlank String? = null
    var employeeCode: String? = null
    @field:Email(message = "user.email")
    var email: String? = null
    var phoneNumber: String? = null
    var fullName: String? = null
    var dateOfBirth: LocalDate? = null
    var avatar: String? = null
    var status: Short? = null
    var roleIds: Set<String>? = null
    var positions: Set<String>? = null
}