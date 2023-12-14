package com.kcvn.spm.common.payload.request

import jakarta.validation.constraints.NotBlank

class LoginRequest {
    var username: @NotBlank String? = null
    var password: @NotBlank String? = null
}