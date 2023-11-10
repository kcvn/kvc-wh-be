package com.kcvn.spm.controller

import com.kcvn.spm.model.tables.pojos.Roles
import com.kcvn.spm.model.tables.pojos.Users
import com.kcvn.spm.payload.request.UserRequest
import com.kcvn.spm.payload.response.MessageResponse
import com.kcvn.spm.payload.response.UserResponse
import com.kcvn.spm.security.ERole
import com.kcvn.spm.service.RoleService
import com.kcvn.spm.service.UserService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.web.bind.annotation.*
import java.util.function.Consumer

@RestController
@RequestMapping("/api/user")
class UserController(
    private val userService: UserService,
    private val roleService: RoleService,
    private val encoder: PasswordEncoder
) {
    @GetMapping("/all")
    fun getAllUsers(@RequestParam(required = false) uname: String?): ResponseEntity<List<UserResponse>?> {
        return try {
            val users: MutableList<UserResponse> = mutableListOf()
            if (uname == null) {
                userService.findAll().forEach { u ->
                    users.add(UserResponse(
                        u.userId!!,
                        u.username!!,
                        roleService.findByUser(u.userId!!).map { it.roleName!! }
                    ))
                }
            } else {
                userService.findByUsernameContaining(uname).forEach { u ->
                    users.add(UserResponse(
                        u.userId!!,
                        u.username!!,
                        roleService.findByUser(u.userId!!).map { it.roleName!! }
                    ))
                }
            }

            if (users.isEmpty()) ResponseEntity<List<UserResponse>?>(HttpStatus.NO_CONTENT)
            else ResponseEntity<List<UserResponse>?>(users, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity<List<UserResponse>?>(null, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/{id}")
    fun getUserById(@PathVariable("id") id: Long): ResponseEntity<UserResponse?> {
        val user = userService.findById(id)
        return if (user != null) {
            ResponseEntity<UserResponse?>(UserResponse(
                user.userId!!,
                user.username!!,
                roleService.findByUser(user.userId!!).map { it.roleName!! }
            ), HttpStatus.OK)
        } else {
            ResponseEntity<UserResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    fun registerUser(@RequestBody userRequest: @Valid UserRequest?): ResponseEntity<*> {
        if (userService.existsByUsername(userRequest?.username.toString()) ?: false) {
            return ResponseEntity.badRequest().body(MessageResponse("Error: Username is already taken!"))
        }

        // Create new user's account
        val user = Users(
            null,
            userRequest?.username,
            encoder.encode(userRequest?.password)
        )
        val strRoles: Set<String>? = userRequest?.role
        val roles: MutableSet<Roles> = HashSet<Roles>()
        if (strRoles == null) {
            val userRole: Roles = roleService.findByName(ERole.ROLE_USER)
                ?: throw RuntimeException("Error: Role is not found.")
            roles.add(userRole)
        } else {
            strRoles.forEach(Consumer<String> { role: String? ->
                when (role) {
                    "admin" -> {
                        val adminRole: Roles = roleService.findByName(ERole.ROLE_ADMIN)
                            ?: throw RuntimeException("Error: Role is not found.")
                        roles.add(adminRole)
                    }

                    // "user"
                    else -> {
                        val userRole: Roles = roleService.findByName(ERole.ROLE_USER)
                            ?: throw RuntimeException("Error: Role is not found.")
                        roles.add(userRole)
                    }
                }
            })
        }
        val userId = userService.save(user)
        if (userId != null) roleService.saveUserRoles(userId, roles)
        return ResponseEntity<UserResponse>(UserResponse(userId!!, user.username!!, roles = strRoles?.toList() ?: listOf()), HttpStatus.CREATED)
    }

    @PutMapping("/update/{id}")
    fun updateUser(@PathVariable("id") id: Long, @RequestBody userRequest: @Valid UserRequest): ResponseEntity<UserResponse?> {
        val user = userService.findById(id)
        if (user != null) {
            user.username = userRequest.username
            user.password = encoder.encode(userRequest.password)
            val response: UserResponse
            userService.update(user).let {
                response = UserResponse(
                    user.userId!!,
                    user.username!!,
                    roleService.findByUser(user.userId!!).map { it.roleName!! }
                )
            }
            return ResponseEntity<UserResponse?>(response, HttpStatus.OK)
        } else {
            return ResponseEntity<UserResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @DeleteMapping("/delete/{id}")
    fun deleteUser(@PathVariable("id") id: Long): ResponseEntity<HttpStatus> {
        return try {
            userService.deleteById(id)
            ResponseEntity<HttpStatus>(HttpStatus.NO_CONTENT)
        } catch (e: Exception) {
            ResponseEntity<HttpStatus>(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}