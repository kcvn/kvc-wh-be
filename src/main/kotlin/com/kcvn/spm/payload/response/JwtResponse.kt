package com.kcvn.spm.payload.response

class JwtResponse(
    var accessToken: String,
    var id: Long,
    var username: String,
    val roles: List<String>
) {
    var tokenType = "Bearer"
}