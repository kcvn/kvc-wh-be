package com.kcvn.spm.app.auth.controller

import com.kcvn.spm.app.auth.payload.request.PasswordRequest
import com.kcvn.spm.app.auth.payload.request.UpdateUserRequest
import com.kcvn.spm.app.auth.payload.request.UserRequest
import com.kcvn.spm.app.auth.payload.response.UserResponse
import com.kcvn.spm.app.auth.security.jwt.JwtUtils
import com.kcvn.spm.app.auth.security.service.UserDetailsImpl
import com.kcvn.spm.app.auth.service.UserService
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.domain.Sort
import org.springframework.data.web.PageableDefault
import org.springframework.data.web.SortDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.security.core.Authentication
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
    private val jwtUtils: JwtUtils
)
{

    @GetMapping("/profile")
    fun getUserProfile(): ResponseEntity<UserResponse?> {
        val userId = jwtUtils.getCurrentUser().getId()
        val user = userService.findById(userId)
        return if (user != null) {
            ResponseEntity<UserResponse?>(user, HttpStatus.OK)
        } else {
            ResponseEntity<UserResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @GetMapping("/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).VIEW_USER.value) || hasRole('ADMIN')")
    fun getAllUsers(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = 10, page = 0)
        @SortDefault.SortDefaults(SortDefault(sort = ["createdDate"], direction = Sort.Direction.DESC))
        pageable: Pageable?
    ): ResponseEntity<*> {
        val result = userService.getPaginatedUsers(search, pageable!!)
        return if (result.data.isEmpty())
            ResponseEntity<Any?>(HttpStatus.NO_CONTENT)
        else
            ResponseEntity<PaginatedResponse>(result, HttpStatus.OK)
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == principal.id || hasAuthority(T(com.kcvn.spm.common.enums.EPermission).VIEW_USER.value) || hasRole('ADMIN')")
    fun getUserById(@PathVariable("id") id: String): ResponseEntity<UserResponse?> {
        val user = userService.findById(id)
        return if (user != null) {
            ResponseEntity<UserResponse?>(user, HttpStatus.OK)
        } else {
            ResponseEntity<UserResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_USER.value) || hasRole('ADMIN')")
    fun createUser(@Valid @RequestBody userRequest: UserRequest?): ResponseEntity<*> {
        val user = userService.createUser(userRequest!!)
        return if (user != null) {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.succeeded"), user),
                HttpStatus.CREATED
            )
        } else {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.failed")),
                HttpStatus.BAD_REQUEST
            )
        }
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("#id == principal.id || hasAuthority(T(com.kcvn.spm.common.enums.EPermission).UPDATE_USER.value) || hasRole('ADMIN')")
    fun updateUser(
        @PathVariable("id") id: String,
        @Valid @RequestBody userRequest: UpdateUserRequest
    ): ResponseEntity<*> {
        val user = userService.updateInfo(id, userRequest)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("update.succeeded"), user),
            HttpStatus.OK
        )
    }

    @PutMapping("/change-password/{id}")
    @PreAuthorize("#id == principal.id || hasAuthority(T(com.kcvn.spm.common.enums.EPermission).UPDATE_USER.value) || hasRole('ADMIN')")
    fun changePassword(
        @PathVariable("id") id: String,
        @Valid @RequestBody passwordRequest: PasswordRequest,
        authentication: Authentication
    ): ResponseEntity<*> {
        val userDetails = authentication.principal as UserDetailsImpl
        if (userDetails.getId() == id && !userService.validateOldPassword(id, passwordRequest.oldPassword!!)) {
            return ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("login.error.wrongPassword")),
                HttpStatus.BAD_REQUEST
            )
        }
        val user = userService.updatePassword(id, passwordRequest.password!!)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("update.succeeded"), user),
            HttpStatus.OK
        )
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).DELETE_USER.value) || hasRole('ADMIN')")
    fun deleteUser(@PathVariable("id") id: String): ResponseEntity<*> {
        userService.deleteById(id)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("delete.succeeded")),
            HttpStatus.OK
        )
    }
}