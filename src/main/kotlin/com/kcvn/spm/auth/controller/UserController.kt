package com.kcvn.spm.auth.controller

import com.kcvn.spm.auth.payload.request.PasswordRequest
import com.kcvn.spm.auth.payload.request.UserRequest
import com.kcvn.spm.auth.payload.response.UserResponse
import com.kcvn.spm.auth.security.service.UserDetailsImpl
import com.kcvn.spm.auth.service.UserService
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import jakarta.validation.Valid
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*


@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
    private val messageSource: MessageSource
) {
    @GetMapping("/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.auth.security.EPermission).VIEW_USER.value) || hasRole('ADMIN')")
    fun getAllUsers(
        @RequestParam(required = false) search: String?,
        @RequestParam(required = false) page: Int?,
        @RequestParam(required = false) size: Int?
    ): ResponseEntity<*> {
        return try {
            if (page != null && size != null) {
                val result = userService.getPaginatedUsers(search, page, size)
                if (result.data.isEmpty())
                    ResponseEntity<Any?>(HttpStatus.NO_CONTENT)
                else
                    ResponseEntity<PaginatedResponse>(result, HttpStatus.OK)
            } else {
                val users: List<UserResponse> = userService.getUsers(search)
                if (users.isEmpty())
                    ResponseEntity<Any?>(HttpStatus.NO_CONTENT)
                else
                    ResponseEntity<List<UserResponse>>(users, HttpStatus.OK)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ResponseEntity<Any?>(e.localizedMessage, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == principal.id || hasAuthority(T(com.kcvn.spm.auth.security.EPermission).VIEW_USER.value) || hasRole('ADMIN')")
    fun getUserById(@PathVariable("id") id: String): ResponseEntity<UserResponse?> {
        val user = userService.findById(id)
        return if (user != null) {
            ResponseEntity<UserResponse?>(user, HttpStatus.OK)
        } else {
            ResponseEntity<UserResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.auth.security.EPermission).CREATE_USER.value) || hasRole('ADMIN')")
    fun createUser(@RequestBody userRequest: @Valid UserRequest?): ResponseEntity<*> {
        val user = userService.createUser(userRequest!!)
        return if (user != null) {
            ResponseEntity<MessageResponse>(
                MessageResponse(
                    messageSource.getMessage("action.succeeded", null, LocaleContextHolder.getLocale()),
                    user
                ),
                HttpStatus.CREATED
            )
        } else {
            ResponseEntity<MessageResponse>(
                MessageResponse(messageSource.getMessage("action.failed", null, LocaleContextHolder.getLocale())),
                HttpStatus.BAD_REQUEST
            )
        }
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("#id == principal.id || hasAuthority(T(com.kcvn.spm.auth.security.EPermission).UPDATE_USER.value) || hasRole('ADMIN')")
    fun updateUser(
        @PathVariable("id") id: String,
        @RequestBody userRequest: @Valid UserRequest
    ): ResponseEntity<*> {
        val user = userService.updateInfo(id, userRequest)
        return ResponseEntity<MessageResponse>(
            MessageResponse(
                messageSource.getMessage("action.succeeded", null, LocaleContextHolder.getLocale()),
                user
            ),
            HttpStatus.OK
        )
    }

    @PutMapping("/change-password/{id}")
    @PreAuthorize("#id == principal.id || hasAuthority(T(com.kcvn.spm.auth.security.EPermission).UPDATE_USER.value) || hasRole('ADMIN')")
    fun changePassword(
        @PathVariable("id") id: String,
        @RequestBody passwordRequest: @Valid PasswordRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        val userDetails = authentication.principal as UserDetailsImpl
        if (userDetails.getId().equals(id)) {
            // user change password him/herself, validate old password first
            if (!userService.validateOldPassword(id, passwordRequest.oldPassword!!)) {
                return ResponseEntity<MessageResponse>(
                    MessageResponse("Wrong password!"),
                    HttpStatus.BAD_REQUEST
                )
            }
        }
        val user = userService.updatePassword(id, passwordRequest.password!!)
        return ResponseEntity<MessageResponse>(
            MessageResponse(
                messageSource.getMessage("action.succeeded", null, LocaleContextHolder.getLocale()),
                user
            ),
            HttpStatus.OK
        )
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.auth.security.EPermission).DELETE_USER.value) || hasRole('ADMIN')")
    fun deleteUser(@PathVariable("id") id: String): ResponseEntity<*> {
        userService.deleteById(id)
        return ResponseEntity<MessageResponse>(
            MessageResponse(messageSource.getMessage("action.succeeded", null, LocaleContextHolder.getLocale())),
            HttpStatus.OK
        )
    }
}