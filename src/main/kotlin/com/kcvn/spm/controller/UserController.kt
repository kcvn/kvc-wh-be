package com.kcvn.spm.controller

import com.kcvn.spm.payload.request.UserRequest
import com.kcvn.spm.payload.response.UserResponse
import com.kcvn.spm.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/user")
class UserController(private val userService: UserService) {
    @GetMapping("/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.security.EPermission).VIEW_USER.value) || hasRole('ADMIN')")
    fun getAllUsers(@RequestParam(required = false) uName: String?): ResponseEntity<List<UserResponse>?> {
        return try {
            val users: List<UserResponse> = userService.getUsers(uName)

            if (users.isEmpty())
                ResponseEntity<List<UserResponse>?>(HttpStatus.NO_CONTENT)
            else
                ResponseEntity<List<UserResponse>?>(users, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity<List<UserResponse>?>(null, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.security.EPermission).VIEW_USER.value) || hasRole('ADMIN')")
    fun getUserById(@PathVariable("id") id: Long): ResponseEntity<UserResponse?> {
        val user = userService.findById(id)
        return if (user != null) {
            ResponseEntity<UserResponse?>(user, HttpStatus.OK)
        } else {
            ResponseEntity<UserResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.security.EPermission).CREATE_USER.value) || hasRole('ADMIN')")
    fun createUser(@RequestBody userRequest: @Valid UserRequest?): ResponseEntity<*> {
        val user = userService.createUser(userRequest!!)
        return if (user != null) {
            ResponseEntity<UserResponse?>(user, HttpStatus.CREATED)
        } else {
            ResponseEntity<UserResponse?>(HttpStatus.BAD_REQUEST)
        }
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.security.EPermission).UPDATE_USER.value) || hasRole('ADMIN')")
    fun updateUser(
        @PathVariable("id") id: Long,
        @RequestBody userRequest: @Valid UserRequest
    ): ResponseEntity<UserResponse?> {
        val user = userService.update(id, userRequest)
        return if (user != null) {
            ResponseEntity<UserResponse?>(user, HttpStatus.OK)
        } else {
            ResponseEntity<UserResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.security.EPermission).DELETE_USER.value) || hasRole('ADMIN')")
    fun deleteUser(@PathVariable("id") id: Long): ResponseEntity<HttpStatus> {
        return try {
            userService.deleteById(id)
            ResponseEntity<HttpStatus>(HttpStatus.NO_CONTENT)
        } catch (e: Exception) {
            ResponseEntity<HttpStatus>(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}