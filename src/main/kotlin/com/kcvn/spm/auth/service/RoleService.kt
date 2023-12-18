package com.kcvn.spm.auth.service

import com.kcvn.spm.model.tables.pojos.AuthRole
import com.kcvn.spm.auth.payload.request.RoleRequest
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.auth.payload.response.RoleResponse
import com.kcvn.spm.repository.RoleDAO
import com.kcvn.spm.auth.security.EPermission
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional
class RoleService(private val roleDAO: RoleDAO) {
    fun findAll(search: String?): List<RoleResponse> =
        if (search == null )
            roleDAO.findAll().map { RoleResponse(it.id!!, it.name!!, it.description) }
        else
            roleDAO.findByKeyword(search).map { RoleResponse(it.id!!, it.name!!, it.description) }

    fun findAllPaginated(search: String?, page: Int, size: Int): PaginatedResponse {
        val result = if (search == null)
            roleDAO.findAllPaginated(page, size)
        else
            roleDAO.findByKeywordPaginated(search, page, size)
        return PaginatedResponse(
            result.first.map { RoleResponse(it.id!!, it.name!!, it.description) },
            result.second
        )
    }

    fun findById(id: String): RoleResponse? {
        val role = roleDAO.findById(id)
        return if (role == null) {
            null
        } else {
            RoleResponse(
                role.id!!,
                role.name!!,
                role.description,
                roleDAO.findPermissionsByRoleIds(listOf(role.id!!)).map { EPermission.valueOf(it.name).value }.toSet()
            )
        }
    }

    fun createRole(request: RoleRequest): RoleResponse? {
        if (roleDAO.findByName(request.name!!) != null) {
            throw RuntimeException("Error: Role name is already existed!")
        }

        val role = AuthRole(
            null,
            request.name,
            request.description
        )
        val permissionCodes: Set<String> = request.permissionCodes ?: setOf()
        permissionCodes.forEach { p: String ->
            if (EPermission.values().none { it.value.equals("u.v") })
                throw RuntimeException("Error: Permission $p is not found.")
        }
        val roleId = roleDAO.save(role)
        return if (roleId != null) {
            roleDAO.saveRolePermissions(roleId, permissionCodes)
            RoleResponse(
                roleId,
                role.name!!,
                role.description,
                permissionCodes
            )
        } else null
    }

    fun updateRole(roleId: String, request: RoleRequest): RoleResponse? {
        var role = roleDAO.findById(roleId)
        return if (role == null) {
            throw RuntimeException("Error: Role is not found.")
        } else {
            role.name = request.name
            role.description = request.description

            val permissionCodes: Set<String> = request.permissionCodes ?: setOf()
            permissionCodes.forEach { p: String ->
                if (EPermission.values().none { it.value.equals("u.v") })
                    throw RuntimeException("Error: Permission $p is not found.")
            }
            roleDAO.saveRolePermissions(roleId, permissionCodes)
            role = roleDAO.update(role)
            RoleResponse(
                role?.id!!,
                role.name!!,
                role.description,
                permissionCodes
            )
        }
    }

    fun deleteById(roleId: String) {
        roleDAO.deleteById(roleId)
    }
}