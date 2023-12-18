package com.kcvn.spm.common.controller

import com.kcvn.spm.common.payload.request.UserRequest
import com.kcvn.spm.common.payload.response.MessageResponse
import com.kcvn.spm.common.payload.response.PaginatedResponse
import com.kcvn.spm.common.payload.response.UserResponse
import com.kcvn.spm.common.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(private val userService: UserService) {
    @GetMapping("/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.security.EPermission).VIEW_USER.value) || hasRole('ADMIN')")
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
            ResponseEntity<Any?>(null, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("#id == principal.id || hasAuthority(T(com.kcvn.spm.common.security.EPermission).VIEW_USER.value) || hasRole('ADMIN')")
    fun getUserById(@PathVariable("id") id: String): ResponseEntity<UserResponse?> {
        val user = userService.findById(id)
        return if (user != null) {
            ResponseEntity<UserResponse?>(user, HttpStatus.OK)
        } else {
            ResponseEntity<UserResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.security.EPermission).CREATE_USER.value) || hasRole('ADMIN')")
    fun createUser(@RequestBody userRequest: @Valid UserRequest?): ResponseEntity<*> {
        val user = userService.createUser(userRequest!!)
        return if (user != null) {
            ResponseEntity<MessageResponse>(MessageResponse(user, "Action succeeded!"), HttpStatus.CREATED)
        } else {
            ResponseEntity<MessageResponse>(MessageResponse(user, "Action failed!"), HttpStatus.BAD_REQUEST)
        }
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("#id == principal.id || hasAuthority(T(com.kcvn.spm.common.security.EPermission).UPDATE_USER.value) || hasRole('ADMIN')")
    fun updateUser(
        @PathVariable("id") id: String,
        @RequestBody userRequest: @Valid UserRequest
    ): ResponseEntity<*> {
        val user = userService.updateInfo(id, userRequest)
        return if (user != null) {
            ResponseEntity<MessageResponse>(MessageResponse(user, "Action succeeded!"), HttpStatus.OK)
        } else {
            ResponseEntity<MessageResponse>(MessageResponse(user, "Action failed!"), HttpStatus.BAD_REQUEST)
        }
    }

    @PutMapping("/change-password/{id}")
    @PreAuthorize("#id == principal.id || hasAuthority(T(com.kcvn.spm.common.security.EPermission).UPDATE_USER.value) || hasRole('ADMIN')")
    fun changePassword(
        @PathVariable("id") id: String,
        @RequestBody userRequest: @Valid UserRequest
    ): ResponseEntity<*> {
        val user = userService.updatePassword(id, userRequest)
        return if (user != null) {
            ResponseEntity<MessageResponse>(MessageResponse(user, "Action succeeded!"), HttpStatus.OK)
        } else {
            ResponseEntity<MessageResponse>(MessageResponse(user, "Action failed!"), HttpStatus.BAD_REQUEST)
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.security.EPermission).DELETE_USER.value) || hasRole('ADMIN')")
    fun deleteUser(@PathVariable("id") id: String): ResponseEntity<*> {
        return try {
            userService.deleteById(id)
            ResponseEntity<MessageResponse>(MessageResponse(null, "Action succeeded!"), HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity<MessageResponse>(MessageResponse(null, "Action failed!"), HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}