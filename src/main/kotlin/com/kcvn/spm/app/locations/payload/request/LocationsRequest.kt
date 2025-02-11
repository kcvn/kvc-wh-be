package com.kcvn.spm.app.locations.payload.request

import jakarta.validation.constraints.NotBlank

class LocationsRequest {
    @field:NotBlank(message = "code must not be blank")
    var code: String? = null
}