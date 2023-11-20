package com.kcvn.spm.payload.request

import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size

class FunctionRequest (
    var name: @NotBlank @Size(max = 100) String? = null
)