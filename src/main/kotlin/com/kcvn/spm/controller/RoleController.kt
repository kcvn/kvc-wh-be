package com.kcvn.spm.controller

import com.kcvn.spm.payload.request.RoleRequest
import com.kcvn.spm.payload.response.RoleResponse
import com.kcvn.spm.service.RoleService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/role")
class RoleController(
    private val roleService: RoleService
) {
    @GetMapping("/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.security.EPermission).VIEW_ROLE.value) || hasRole('ADMIN')")
    fun getAllRoles(): ResponseEntity<List<RoleResponse>?> {
        return try {
            val roles: List<RoleResponse> = roleService.findAll()

            if (roles.isEmpty()) ResponseEntity<List<RoleResponse>?>(HttpStatus.NO_CONTENT)
            else ResponseEntity<List<RoleResponse>?>(roles, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity<List<RoleResponse>?>(null, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.security.EPermission).VIEW_ROLE.value) || hasRole('ADMIN')")
    fun getRoleById(@PathVariable("id") id: Int): ResponseEntity<RoleResponse?> {
        val role = roleService.findById(id)
        return if (role != null) {
            ResponseEntity<RoleResponse?>(role, HttpStatus.OK)
        } else {
            ResponseEntity<RoleResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.security.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun createRole(@RequestBody request: @Valid RoleRequest?): ResponseEntity<*> {
        val role = roleService.createRole(request!!)
        return if (role == null) {
            ResponseEntity<RoleResponse?>(HttpStatus.BAD_REQUEST)
        } else {
            ResponseEntity<RoleResponse>(role, HttpStatus.CREATED)
        }
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.security.EPermission).UPDATE_ROLE.value) || hasRole('ADMIN')")
    fun updateRole(
        @PathVariable("id") id: Int,
        @RequestBody request: @Valid RoleRequest
    ): ResponseEntity<RoleResponse?> {
        val role = roleService.updateRole(id, request)
        return if (role == null) {
            ResponseEntity<RoleResponse?>(HttpStatus.NOT_FOUND)
        } else {
            ResponseEntity<RoleResponse?>(role, HttpStatus.OK)
        }
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.security.EPermission).DELETE_ROLE.value) || hasRole('ADMIN')")
    fun deleteRole(@PathVariable("id") id: Int): ResponseEntity<HttpStatus> {
        return try {
            roleService.deleteById(id)
            ResponseEntity<HttpStatus>(HttpStatus.NO_CONTENT)
        } catch (e: Exception) {
            ResponseEntity<HttpStatus>(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}