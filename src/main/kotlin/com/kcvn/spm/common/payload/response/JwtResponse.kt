package com.kcvn.spm.common.payload.response

class JwtResponse(
    var accessToken: String,
    var id: String,
    var username: String
) {
    var tokenType = "Bearer"
}