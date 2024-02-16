package com.kcvn.spm.app.auth.controller


import com.kcvn.spm.app.auth.payload.request.ForgotPasswordRequest
import com.kcvn.spm.app.auth.payload.request.LoginRequest
import com.kcvn.spm.app.auth.payload.request.PasswordRequest
import com.kcvn.spm.app.auth.payload.response.JwtResponse
import com.kcvn.spm.app.auth.security.jwt.JwtUtils
import com.kcvn.spm.app.auth.security.service.UserDetailsImpl
import com.kcvn.spm.app.auth.service.UserService
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.authentication.AuthenticationManager
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import java.util.*

@RestController
@RequestMapping("/api/auth")
class AuthController(
        private val authenticationManager: AuthenticationManager,
        private val jwtUtils: JwtUtils,
        private val userService: UserService
) {
    @PostMapping("/signin")
    fun authenticateUser(@Valid @RequestBody loginRequest: LoginRequest?): ResponseEntity<*> {
            val authentication: Authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken(loginRequest?.username, loginRequest?.password)
            )
            SecurityContextHolder.getContext().authentication = authentication
            val jwt = jwtUtils.generateJwtToken(authentication)
            val userDetails = authentication.principal as UserDetailsImpl
            return ResponseEntity.ok(
                    JwtResponse(
                            jwt,
                            userDetails.getId(),
                            userDetails.username
                    )
            )
    }

    @PostMapping("/forgot-password")
    fun forgotPassword(@Valid @RequestBody request: ForgotPasswordRequest?): ResponseEntity<*> {
        userService.createPasswordResetToken(request!!.email!!, request.url!!)
        return ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.succeeded")),
                HttpStatus.OK
        )
    }

    @PostMapping("/reset-password")
    fun resetPassword(locale: Locale?, @Valid @RequestBody passwordRequest: PasswordRequest): ResponseEntity<*> {
        userService.validatePasswordResetToken(passwordRequest.token!!)
        val user = userService.getUserByPasswordResetToken(passwordRequest.token!!)
        userService.updatePassword(user.id!!, passwordRequest.password!!)
        return ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.succeeded")),
                HttpStatus.OK
        )
    }
}