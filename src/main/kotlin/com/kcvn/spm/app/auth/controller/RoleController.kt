package com.kcvn.spm.app.auth.controller

import com.kcvn.spm.app.auth.payload.request.RoleRequest
import com.kcvn.spm.app.auth.payload.response.RoleResponse
import com.kcvn.spm.app.auth.service.RoleService
import com.kcvn.spm.common.constants.PagingDefault
import com.kcvn.spm.common.payload.MessageResponse
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.common.util.CommonUtils
import jakarta.validation.Valid
import org.springframework.data.domain.Pageable
import org.springframework.data.web.PageableDefault
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.access.prepost.PreAuthorize
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/role")
class RoleController(private val roleService: RoleService) {
    @GetMapping("/all")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).VIEW_ROLE.value) || hasRole('ADMIN')")
    fun getAllRoles(
        @RequestParam(required = false) search: String?,
        @PageableDefault(size = PagingDefault.SIZE, page = PagingDefault.PAGE) pageable: Pageable?
    ): ResponseEntity<*> {
        val result = roleService.findAllPaginated(search, pageable!!)
        return if (result.data.isEmpty())
            ResponseEntity<Any?>(HttpStatus.NO_CONTENT)
        else
            ResponseEntity<PaginatedResponse>(result, HttpStatus.OK)
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).VIEW_ROLE.value) || hasRole('ADMIN')")
    fun getRoleById(@PathVariable("id") id: String): ResponseEntity<RoleResponse?> {
        val role = roleService.findById(id)
        return if (role != null) {
            ResponseEntity<RoleResponse?>(role, HttpStatus.OK)
        } else {
            ResponseEntity<RoleResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).CREATE_ROLE.value) || hasRole('ADMIN')")
    fun createRole(@Valid @RequestBody request: RoleRequest?): ResponseEntity<*> {
        val role = roleService.createRole(request!!)
        return if (role == null) {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.failed")),
                HttpStatus.BAD_REQUEST
            )
        } else {
            ResponseEntity<MessageResponse>(
                MessageResponse(CommonUtils.getMessage("action.succeeded"), role),
                HttpStatus.CREATED
            )
        }
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).UPDATE_ROLE.value) || hasRole('ADMIN')")
    fun updateRole(
        @PathVariable("id") id: String,
        @Valid @RequestBody request: RoleRequest
    ): ResponseEntity<*> {
        val role = roleService.updateRole(id, request)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded"), role),
            HttpStatus.OK
        )
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasAuthority(T(com.kcvn.spm.common.enums.EPermission).DELETE_ROLE.value) || hasRole('ADMIN')")
    fun deleteRole(@PathVariable("id") id: String): ResponseEntity<*> {
        roleService.deleteById(id)
        return ResponseEntity<MessageResponse>(
            MessageResponse(CommonUtils.getMessage("action.succeeded")),
            HttpStatus.OK
        )
    }
}