package com.kcvn.spm.controller

import com.kcvn.spm.payload.request.LoginRequest
import com.kcvn.spm.payload.response.JwtResponse
import com.kcvn.spm.security.jwt.JwtUtils
import com.kcvn.spm.security.service.UserDetailsImpl
import com.kcvn.spm.service.RoleService
import com.kcvn.spm.service.UserService
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.stream.Collectors

@RestController
@RequestMapping("/api/auth")
class AuthController(
    private val authenticationManager: AuthenticationManager,
    private val jwtUtils: JwtUtils
) {
    @PostMapping("/signin")
    fun authenticateUser(@RequestBody loginRequest: @Valid LoginRequest?): ResponseEntity<*> {
        val authentication: Authentication = authenticationManager
            .authenticate(UsernamePasswordAuthenticationToken(loginRequest?.username, loginRequest?.password))
        SecurityContextHolder.getContext().authentication = authentication
        val jwt = jwtUtils.generateJwtToken(authentication)
        val userDetails = authentication.getPrincipal() as UserDetailsImpl
        val roles = userDetails.authorities.stream().map { item: GrantedAuthority -> item.authority }
            .collect(Collectors.toList())
        return ResponseEntity
            .ok(JwtResponse(jwt, userDetails.getId(), userDetails.username, roles))
    }
}