package com.kcvn.spm.app.auth.payload.request

import jakarta.validation.constraints.NotBlank

class LoginRequest {
    @field:NotBlank(message = "username must not be blank")
    var username: String? = null
    @field:NotBlank(message = "password must not be blank")
    var password: String? = null
}