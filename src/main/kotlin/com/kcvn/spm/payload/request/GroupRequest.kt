package com.kcvn.spm.payload.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class GroupRequest {
    var name: @NotBlank @Size(min = 3, max = 50) String? = null
    var description: @NotBlank @Size(max = 100) String? = null
}