package com.kcvn.spm.auth.payload.request

import jakarta.validation.constraints.NotBlank

class PasswordRequest {
    var token: String? = null
    var oldPassword: String? = null
    var password: @NotBlank String? = null
}