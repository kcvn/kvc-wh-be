package com.kcvn.spm.auth.service

import com.kcvn.spm.model.tables.pojos.AuthRole
import com.kcvn.spm.auth.payload.request.RoleRequest
import com.kcvn.spm.common.payload.PaginatedResponse
import com.kcvn.spm.auth.payload.response.RoleResponse
import com.kcvn.spm.repository.RoleDAO
import com.kcvn.spm.auth.security.EPermission
import com.kcvn.spm.common.exception.BusinessException
import org.springframework.context.MessageSource
import org.springframework.context.i18n.LocaleContextHolder
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
            throw BusinessException("Error: Role name is already existed!")
        }

        val role = AuthRole(
            null,
            request.name,
            request.description
        )
        val permissionCodes: Set<String> = request.permissionCodes ?: setOf()
        permissionCodes.forEach { p: String ->
            if (EPermission.values().none { it.value.equals(p) })
                throw BusinessException("Error: Permission $p is not found.")
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

    fun updateRole(roleId: String, request: RoleRequest): RoleResponse {
        val role = roleDAO.findById(roleId) ?: throw BusinessException("Error: Role is not found.")
        role.name = request.name
        role.description = request.description

        val permissionCodes: Set<String> = request.permissionCodes ?: setOf()
        permissionCodes.forEach { p: String ->
            if (EPermission.values().none { it.value.equals(p) })
                throw BusinessException("Error: Permission $p is not found.")
        }
        roleDAO.saveRolePermissions(roleId, permissionCodes)
        roleDAO.update(role)
        return RoleResponse(
            role.id!!,
            role.name!!,
            role.description,
            permissionCodes
        )
    }

    fun deleteById(roleId: String) {
        if (roleDAO.isRoleUsed(roleId))
            throw BusinessException("Role is in use!")
        roleDAO.deleteById(roleId)
    }
}