package com.kcvn.spm.auth.payload.response

class JwtResponse(
    var accessToken: String,
    var id: String,
    var username: String
) {
    var tokenType = "Bearer"
}