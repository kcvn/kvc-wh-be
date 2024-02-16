package com.kcvn.spm.app.auth.payload.request

import jakarta.validation.constraints.NotBlank

class PasswordRequest {
    var token: String? = null
    var oldPassword: String? = null
    @field:NotBlank(message = "password must not be blank")
    var password: String? = null
}