package com.kcvn.spm.service

import com.kcvn.spm.model.tables.pojos.Permissions
import com.kcvn.spm.model.tables.pojos.Roles
import com.kcvn.spm.payload.request.RoleRequest
import com.kcvn.spm.payload.response.RoleResponse
import com.kcvn.spm.repository.PermissionDAO
import com.kcvn.spm.repository.RoleDAO
import com.kcvn.spm.security.EPermission
import com.kcvn.spm.security.ERole
import org.springframework.stereotype.Service

@Service
class RoleService(private val roleDAO: RoleDAO, private val permissionDAO: PermissionDAO) {
    fun findAll(): List<RoleResponse> = roleDAO.findAll().map { RoleResponse(it.roleId!!, it.roleName!!, it.roleDescription) }

    fun findById(id: Int): RoleResponse? {
        val role = roleDAO.findById(id)
        return if (role == null) {
            null
        } else {
            RoleResponse(
                role.roleId!!,
                role.roleName!!,
                role.roleDescription,
                permissionDAO.findByRoleIds(listOf(role.roleId!!)).map { EPermission.valueOf(it.permissionName!!).value }
            )
        }
    }

    fun createRole(request: RoleRequest): RoleResponse? {
        if (roleDAO.findByName(request.name!!) != null) {
            throw RuntimeException("Error: Role name is already existed!")
        }

        val role = Roles(
            null,
            request.name,
            request.description
        )
        val strPermissions: Set<String> = request.permissions ?: setOf()
        val permissions: MutableSet<Permissions> = mutableSetOf()
        val allPermissions = permissionDAO.findAll()
        strPermissions.forEach { p: String ->
            val permission: Permissions = allPermissions.find { EPermission.valueOf(it.permissionName!!).value.equals(p) }
                ?: throw RuntimeException("Error: Permission is not found.")
            permissions.add(permission)
        }
        val roleId = roleDAO.save(role)
        return if (roleId != null) {
            permissionDAO.saveRolePermissions(roleId, permissions)
            RoleResponse(
                roleId,
                role.roleName!!,
                role.roleDescription
            )
        } else null
    }

    fun updateRole(roleId: Int, request: RoleRequest): RoleResponse? {
        var role = roleDAO.findById(roleId)
        return if(role == null) {
            null
        } else {
            role.roleName = request.name
            role.roleDescription = request.description

            val strPermissions: Set<String> = request.permissions ?: setOf()
            val permissions: MutableSet<Permissions> = mutableSetOf()
            val allPermissions = permissionDAO.findAll()
            strPermissions.forEach { p: String ->
                val permission: Permissions = allPermissions.find { EPermission.valueOf(it.permissionName!!).value.equals(p) }
                    ?: throw RuntimeException("Error: Permission is not found.")
                permissions.add(permission)
            }
            permissionDAO.saveRolePermissions(roleId, permissions)
            role = roleDAO.update(role)
            RoleResponse(
                role?.roleId!!,
                role.roleName!!,
                role.roleDescription
            )
        }
    }

    fun deleteById(roleId: Int) {
        val role = roleDAO.findById(roleId)
        if (role != null && role.roleName.equals(ERole.ROLE_ADMIN.name)) {
            throw RuntimeException("Error: Cannot delete Admin role!")
        }
        if (role != null && role.roleName.equals(ERole.ROLE_USER.name)) {
            throw RuntimeException("Error: Cannot delete User role!")
        }
        roleDAO.deleteById(roleId)
    }
}