package com.kcvn.spm.payload.response

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

data class PermissionResponse(
    var id: Int,
    var name: String,
    var description: String?
)