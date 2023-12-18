package com.kcvn.spm.auth.payload.request

import jakarta.validation.constraints.NotBlank

class LoginRequest {
    var username: @NotBlank String? = null
    var password: @NotBlank String? = null
}