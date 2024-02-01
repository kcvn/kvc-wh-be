package com.kcvn.spm.app.auth.payload.request

import jakarta.validation.constraints.NotBlank

class RoleRequest {
    @field:NotBlank(message = "name must not be blank")
    var name: String? = null
    var description: String? = null
    var permissionCodes: Set<String>? = null
}