package com.kcvn.spm.app.locations.payload.request

import jakarta.validation.constraints.NotBlank

data class LocationsRequest (
    @field:NotBlank(message = "location code must not be blank")
    var locationCode: String? = null
)