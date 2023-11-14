package com.kcvn.spm.controller

import com.kcvn.spm.model.tables.pojos.Permissions
import com.kcvn.spm.payload.request.PermissionRequest
import com.kcvn.spm.payload.response.PermissionResponse
import com.kcvn.spm.service.PermissionService
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/permission")
class PermissionController(
    private val permissionService: PermissionService
) {
    @GetMapping("/all")
    fun getAllPermissions(): ResponseEntity<List<PermissionResponse>?> {
        return try {
            val groups: MutableList<PermissionResponse> = mutableListOf()
            permissionService.findAll().forEach { u ->
                groups.add(
                    PermissionResponse(
                        u.permissionId!!,
                        u.permissionName!!,
                        u.permissionDescription
                    )
                )
            }

            if (groups.isEmpty()) ResponseEntity<List<PermissionResponse>?>(HttpStatus.NO_CONTENT)
            else ResponseEntity<List<PermissionResponse>?>(groups, HttpStatus.OK)
        } catch (e: Exception) {
            ResponseEntity<List<PermissionResponse>?>(null, HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }

    @GetMapping("/{id}")
    fun getGroupById(@PathVariable("id") id: Int): ResponseEntity<PermissionResponse?> {
        val permission = permissionService.findById(id)
        return if (permission != null) {
            ResponseEntity<PermissionResponse?>(
                PermissionResponse(
                    permission.permissionId!!,
                    permission.permissionName!!,
                    permission.permissionDescription
                ), HttpStatus.OK
            )
        } else {
            ResponseEntity<PermissionResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping("/create")
    fun createPermission(@RequestBody request: @Valid PermissionRequest?): ResponseEntity<*> {
        // Create new permission
        val permission = Permissions(
            null,
            request?.name,
            request?.description
        )
        val permissionId = permissionService.save(permission)
        return ResponseEntity<PermissionResponse>(
            PermissionResponse(
                permissionId!!,
                permission.permissionName!!,
                permission.permissionDescription
            ), HttpStatus.CREATED
        )
    }

    @PutMapping("/update/{id}")
    fun updatePermission(
        @PathVariable("id") id: Int,
        @RequestBody request: @Valid PermissionRequest
    ): ResponseEntity<PermissionResponse?> {
        val permission = permissionService.findById(id)
        return if (permission != null) {
            permission.permissionName = request.name
            permission.permissionDescription = request.description
            val response: PermissionResponse
            permissionService.update(permission).let {
                response = PermissionResponse(
                    permission.permissionId!!,
                    permission.permissionName!!,
                    permission.permissionDescription
                )
            }
            ResponseEntity<PermissionResponse?>(response, HttpStatus.OK)
        } else {
            ResponseEntity<PermissionResponse?>(HttpStatus.NOT_FOUND)
        }
    }

    @DeleteMapping("/delete/{id}")
    fun deletePermission(@PathVariable("id") id: Int): ResponseEntity<HttpStatus> {
        return try {
            permissionService.deleteById(id)
            ResponseEntity<HttpStatus>(HttpStatus.NO_CONTENT)
        } catch (e: Exception) {
            ResponseEntity<HttpStatus>(HttpStatus.INTERNAL_SERVER_ERROR)
        }
    }
}