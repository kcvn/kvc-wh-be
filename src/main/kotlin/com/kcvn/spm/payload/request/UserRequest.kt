package com.kcvn.spm.payload.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class UserRequest {
    var username: @NotBlank @Size(min = 3, max = 50) String? = null
    var password: @NotBlank @Size(min = 6, max = 100) String? = null
    var role: Set<String>? = null
}