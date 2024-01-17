package com.kcvn.spm.app.auth.payload.request

import jakarta.validation.constraints.NotBlank

class RoleRequest {
    var name: @NotBlank String? = null
    var description: String? = null
    var permissionCodes: Set<String>? = null
}